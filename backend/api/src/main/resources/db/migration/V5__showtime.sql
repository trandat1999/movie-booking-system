-- M2: lịch chiếu, giá vé, cấu hình hệ thống (SCHADM, SYS01)

CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Giá cơ bản cho 1 chỗ ngồi theo định dạng chiếu và loại ngày
CREATE TABLE ticket_base_prices (
    format     VARCHAR(20) NOT NULL,
    day_type   VARCHAR(20) NOT NULL,
    price      BIGINT      NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (format, day_type),
    CONSTRAINT ck_base_price_format CHECK (format IN ('TWO_D', 'THREE_D', 'IMAX')),
    CONSTRAINT ck_base_price_day CHECK (day_type IN ('WEEKDAY', 'WEEKEND', 'HOLIDAY')),
    CONSTRAINT ck_base_price_positive CHECK (price > 0)
);

INSERT INTO ticket_base_prices (format, day_type, price) VALUES
    ('TWO_D',   'WEEKDAY', 75000),  ('TWO_D',   'WEEKEND', 90000),  ('TWO_D',   'HOLIDAY', 100000),
    ('THREE_D', 'WEEKDAY', 95000),  ('THREE_D', 'WEEKEND', 110000), ('THREE_D', 'HOLIDAY', 120000),
    ('IMAX',    'WEEKDAY', 130000), ('IMAX',    'WEEKEND', 150000), ('IMAX',    'HOLIDAY', 160000);

CREATE TABLE holidays (
    holiday_date DATE         PRIMARY KEY,
    name         VARCHAR(100) NOT NULL
);

-- end_at = start_at + thời lượng phim; blocked_until = end_at + thời gian dọn phòng.
-- Ràng buộc EXCLUDE bảo đảm ở mức CSDL: hai suất chưa hủy trong cùng phòng không chồng thời gian,
-- kể cả khi hai quản trị viên tạo suất cùng lúc (SCHADM04, SCHADM05).
CREATE TABLE showtimes (
    id            BIGSERIAL   PRIMARY KEY,
    movie_id      BIGINT      NOT NULL REFERENCES movies (id),
    room_id       BIGINT      NOT NULL REFERENCES rooms (id),
    start_at      TIMESTAMPTZ NOT NULL,
    end_at        TIMESTAMPTZ NOT NULL,
    blocked_until TIMESTAMPTZ NOT NULL,
    format        VARCHAR(20) NOT NULL,
    audio_type    VARCHAR(20) NOT NULL,
    day_type      VARCHAR(20) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_showtimes_time CHECK (start_at < end_at AND end_at <= blocked_until),
    CONSTRAINT ck_showtimes_format CHECK (format IN ('TWO_D', 'THREE_D', 'IMAX')),
    CONSTRAINT ck_showtimes_audio CHECK (audio_type IN ('SUBTITLED', 'DUBBED')),
    CONSTRAINT ck_showtimes_status CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    CONSTRAINT ex_showtimes_room_overlap EXCLUDE USING gist (
        room_id WITH =,
        tstzrange(start_at, blocked_until, '[)') WITH &&
    ) WHERE (status <> 'CANCELLED')
);
CREATE INDEX ix_showtimes_start ON showtimes (start_at);
CREATE INDEX ix_showtimes_movie_start ON showtimes (movie_id, start_at);

-- Giá đã chốt cho từng loại ghế của suất chiếu (đổi bảng giá sau này không ảnh hưởng suất đã tạo)
CREATE TABLE showtime_prices (
    showtime_id    BIGINT      NOT NULL REFERENCES showtimes (id) ON DELETE CASCADE,
    seat_type_code VARCHAR(20) NOT NULL REFERENCES seat_types (code),
    price          BIGINT      NOT NULL,
    PRIMARY KEY (showtime_id, seat_type_code),
    CONSTRAINT ck_showtime_prices_positive CHECK (price > 0)
);

-- Trạng thái từng ghế theo suất chiếu — nguồn sự thật cho giữ ghế/bán vé (PLAN.md mục 8.1).
-- Cột hold_id/order_id dùng từ M4.
CREATE TABLE showtime_seats (
    showtime_id BIGINT      NOT NULL REFERENCES showtimes (id) ON DELETE CASCADE,
    seat_id     BIGINT      NOT NULL REFERENCES seats (id),
    status      VARCHAR(20) NOT NULL,
    hold_id     BIGINT,
    held_until  TIMESTAMPTZ,
    order_id    BIGINT,
    version     INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (showtime_id, seat_id),
    CONSTRAINT ck_showtime_seats_status CHECK (status IN ('AVAILABLE', 'HELD', 'SOLD', 'BLOCKED'))
);

-- Tham số nghiệp vụ (PLAN.md mục 9). value lưu dạng chuỗi, kiểu và giới hạn do SettingKey kiểm tra.
CREATE TABLE system_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    value       VARCHAR(500) NOT NULL,
    updated_by  BIGINT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO system_settings (setting_key, value) VALUES
    ('hold_ttl_single_sec', '420'),
    ('hold_ttl_group_sec', '600'),
    ('max_seats_per_booking', '8'),
    ('group_max_size', '20'),
    ('max_active_groups_per_user', '3'),
    ('max_active_holds_per_user', '2'),
    ('cleaning_buffer_min', '15'),
    ('cancel_before_hours', '24'),
    ('refund_percent', '100'),
    ('sale_close_after_start_min', '15'),
    ('orphan_seat_rule', 'block'),
    ('group_split_max_rows', '3'),
    ('rsvp_reminder_before_min', '60');
