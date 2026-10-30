-- M2: rạp, phòng chiếu, sơ đồ ghế (CIN01–CIN09)

CREATE TABLE cinemas (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    city        VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20),
    description TEXT,
    image_url   VARCHAR(500),
    amenities   JSONB        NOT NULL DEFAULT '[]',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_cinemas_name UNIQUE (name),
    CONSTRAINT ck_cinemas_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Sơ đồ phòng là lưới grid_rows × grid_cols; ô không có ghế là lối đi/khoảng trống.
-- layout_locked = true khi phòng đã có suất chiếu: từ đó không được vẽ lại sơ đồ (mã ghế phải ổn định),
-- chỉ được bật/tắt từng ghế.
CREATE TABLE rooms (
    id            BIGSERIAL   PRIMARY KEY,
    cinema_id     BIGINT      NOT NULL REFERENCES cinemas (id),
    name          VARCHAR(50) NOT NULL,
    room_type     VARCHAR(20) NOT NULL,
    grid_rows     INT         NOT NULL DEFAULT 0,
    grid_cols     INT         NOT NULL DEFAULT 0,
    layout_locked BOOLEAN     NOT NULL DEFAULT false,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_rooms_cinema_name UNIQUE (cinema_id, name),
    CONSTRAINT ck_rooms_type CHECK (room_type IN ('STANDARD', 'PREMIUM', 'IMAX')),
    CONSTRAINT ck_rooms_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_rooms_grid CHECK (grid_rows BETWEEN 0 AND 30 AND grid_cols BETWEEN 0 AND 40)
);
CREATE INDEX ix_rooms_cinema ON rooms (cinema_id);

-- Danh mục loại ghế cố định. Giá ghế = giá cơ bản × capacity + surcharge (PLAN.md mục 8.6).
CREATE TABLE seat_types (
    code       VARCHAR(20)  PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    capacity   INT          NOT NULL,
    col_span   INT          NOT NULL,
    surcharge  BIGINT       NOT NULL DEFAULT 0,
    color      VARCHAR(7)   NOT NULL,
    sort_order INT          NOT NULL,
    CONSTRAINT ck_seat_types_surcharge CHECK (surcharge >= 0)
);

INSERT INTO seat_types (code, name, capacity, col_span, surcharge, color, sort_order) VALUES
    ('STANDARD',   'Ghế thường',     1, 1, 0,     '#8c8c8c', 1),
    ('VIP',        'Ghế VIP',        1, 1, 20000, '#fa8c16', 2),
    ('COUPLE',     'Ghế đôi',        2, 2, 30000, '#eb2f96', 3),
    ('ACCESSIBLE', 'Ghế khuyết tật', 1, 1, 0,     '#1677ff', 4);

CREATE TABLE seats (
    id             BIGSERIAL   PRIMARY KEY,
    room_id        BIGINT      NOT NULL REFERENCES rooms (id),
    row_label      VARCHAR(3)  NOT NULL,
    number         INT         NOT NULL,
    grid_row       INT         NOT NULL,
    grid_col       INT         NOT NULL,
    col_span       INT         NOT NULL,
    seat_type_code VARCHAR(20) NOT NULL REFERENCES seat_types (code),
    active         BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT uq_seats_label UNIQUE (room_id, row_label, number),
    CONSTRAINT uq_seats_cell UNIQUE (room_id, grid_row, grid_col)
);
CREATE INDEX ix_seats_room ON seats (room_id);
