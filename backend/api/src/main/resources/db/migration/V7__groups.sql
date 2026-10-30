-- M6: đặt vé theo nhóm (PLAN.md 6.3, 7)

CREATE TABLE booking_groups (
    id                  BIGSERIAL    PRIMARY KEY,
    code                VARCHAR(16)  NOT NULL UNIQUE,  -- mã mời
    name                VARCHAR(120) NOT NULL,
    leader_id           BIGINT       NOT NULL REFERENCES users (id),
    movie_id            BIGINT       NOT NULL REFERENCES movies (id),
    cinema_id           BIGINT       REFERENCES cinemas (id),
    date_from           DATE,
    date_to             DATE,
    rsvp_deadline       TIMESTAMPTZ  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    locked_showtime_id  BIGINT       REFERENCES showtimes (id),
    locked_ticket_count INT,
    hold_id             BIGINT       REFERENCES seat_holds (id),
    order_id            BIGINT       REFERENCES orders (id),
    reminder_sent_at    TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_groups_status CHECK (status IN ('OPEN', 'LOCKED', 'HOLDING', 'BOOKED', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT ck_groups_dates CHECK (date_from IS NULL OR date_to IS NULL OR date_from <= date_to),
    CONSTRAINT ck_groups_ticket_count CHECK (locked_ticket_count IS NULL OR locked_ticket_count > 0)
);
CREATE INDEX ix_groups_status_deadline ON booking_groups (status, rsvp_deadline);

CREATE TABLE group_members (
    id          BIGSERIAL   PRIMARY KEY,
    group_id    BIGINT      NOT NULL REFERENCES booking_groups (id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    role        VARCHAR(10) NOT NULL,
    rsvp        VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    guest_count INT         NOT NULL DEFAULT 0,
    max_budget  BIGINT,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    left_at     TIMESTAMPTZ,
    CONSTRAINT uq_group_members UNIQUE (group_id, user_id),
    CONSTRAINT ck_group_members_role CHECK (role IN ('LEADER', 'MEMBER')),
    CONSTRAINT ck_group_members_rsvp CHECK (rsvp IN ('PENDING', 'YES', 'NO', 'MAYBE')),
    CONSTRAINT ck_group_members_guests CHECK (guest_count >= 0),
    CONSTRAINT ck_group_members_budget CHECK (max_budget IS NULL OR max_budget > 0)
);
CREATE INDEX ix_group_members_user ON group_members (user_id) WHERE left_at IS NULL;

CREATE TABLE group_showtime_options (
    id          BIGSERIAL   PRIMARY KEY,
    group_id    BIGINT      NOT NULL REFERENCES booking_groups (id) ON DELETE CASCADE,
    showtime_id BIGINT      NOT NULL REFERENCES showtimes (id),
    proposed_by BIGINT      NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_group_options UNIQUE (group_id, showtime_id)
);

CREATE TABLE group_votes (
    option_id  BIGINT      NOT NULL REFERENCES group_showtime_options (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (option_id, user_id)
);

-- Vé của đơn nhóm được giao cho ai (GRP17)
CREATE TABLE group_seat_assignments (
    id          BIGSERIAL    PRIMARY KEY,
    group_id    BIGINT       NOT NULL REFERENCES booking_groups (id) ON DELETE CASCADE,
    ticket_id   BIGINT       NOT NULL UNIQUE REFERENCES tickets (id),
    user_id     BIGINT       REFERENCES users (id),
    guest_label VARCHAR(100),
    CONSTRAINT ck_assignment_holder CHECK (user_id IS NOT NULL)
);

CREATE TABLE group_events (
    id         BIGSERIAL   PRIMARY KEY,
    group_id   BIGINT      NOT NULL REFERENCES booking_groups (id) ON DELETE CASCADE,
    type       VARCHAR(40) NOT NULL,
    actor_id   BIGINT,
    data       JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_group_events_group ON group_events (group_id, created_at);

-- Phục vụ thống kê ADM02 và thực nghiệm: mỗi lần đề xuất cụm ghế cho nhóm
CREATE TABLE seat_suggestion_logs (
    id          BIGSERIAL   PRIMARY KEY,
    group_id    BIGINT      NOT NULL REFERENCES booking_groups (id) ON DELETE CASCADE,
    showtime_id BIGINT      NOT NULL REFERENCES showtimes (id),
    party_size  INT         NOT NULL,
    options     JSONB       NOT NULL,
    reason      VARCHAR(30),
    chosen_rank INT,
    contiguous  BOOLEAN     NOT NULL,
    duration_ms INT         NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_suggestion_logs_group ON seat_suggestion_logs (group_id, created_at);

ALTER TABLE seat_holds ADD CONSTRAINT fk_seat_holds_group FOREIGN KEY (group_id) REFERENCES booking_groups (id);
ALTER TABLE orders ADD CONSTRAINT fk_orders_group FOREIGN KEY (group_id) REFERENCES booking_groups (id);

-- Trọng số thuật toán cụm ghế (PLAN.md 8.2, thực nghiệm M5)
INSERT INTO system_settings (setting_key, value) VALUES
    ('seat_weight_center', '30'),
    ('seat_weight_rows', '30'),
    ('seat_weight_align', '10'),
    ('seat_weight_orphan', '15'),
    ('seat_weight_price', '5'),
    ('seat_weight_waste', '10');
