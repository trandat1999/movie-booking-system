-- M4: giữ ghế, đơn hàng, thanh toán, vé, thông báo (BKG01–BKG10, NOT01)

-- Một lượt giữ ghế của người dùng cho một suất chiếu. Ghế được giữ nằm ở showtime_seats.hold_id.
CREATE TABLE seat_holds (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    showtime_id BIGINT      NOT NULL REFERENCES showtimes (id),
    group_id    BIGINT,
    status      VARCHAR(20) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_seat_holds_status CHECK (status IN ('ACTIVE', 'CONVERTED', 'EXPIRED', 'RELEASED'))
);
CREATE INDEX ix_seat_holds_user_status ON seat_holds (user_id, status);
CREATE INDEX ix_seat_holds_active_expiry ON seat_holds (expires_at) WHERE status = 'ACTIVE';

CREATE TABLE orders (
    id           BIGSERIAL   PRIMARY KEY,
    code         VARCHAR(20) NOT NULL,
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    showtime_id  BIGINT      NOT NULL REFERENCES showtimes (id),
    hold_id      BIGINT      NOT NULL REFERENCES seat_holds (id),
    group_id     BIGINT,
    status       VARCHAR(20) NOT NULL,
    subtotal     BIGINT      NOT NULL,
    discount     BIGINT      NOT NULL DEFAULT 0,
    total        BIGINT      NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    paid_at      TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_orders_code UNIQUE (code),
    CONSTRAINT uq_orders_hold UNIQUE (hold_id),
    CONSTRAINT ck_orders_status CHECK (status IN
        ('PENDING_PAYMENT', 'PAID', 'EXPIRED', 'CANCELLED', 'REFUND_PENDING', 'REFUNDED', 'COMPLETED')),
    CONSTRAINT ck_orders_total CHECK (total >= 0 AND subtotal >= 0 AND discount >= 0)
);
CREATE INDEX ix_orders_user_created ON orders (user_id, created_at DESC);
CREATE INDEX ix_orders_status ON orders (status);

-- Giá được chụp lại tại thời điểm đặt (BR09)
CREATE TABLE order_items (
    id             BIGSERIAL   PRIMARY KEY,
    order_id       BIGINT      NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    seat_id        BIGINT      NOT NULL REFERENCES seats (id),
    seat_label     VARCHAR(10) NOT NULL,
    seat_type_code VARCHAR(20) NOT NULL,
    price          BIGINT      NOT NULL,
    CONSTRAINT uq_order_items_seat UNIQUE (order_id, seat_id)
);

CREATE TABLE tickets (
    id             BIGSERIAL   PRIMARY KEY,
    order_id       BIGINT      NOT NULL REFERENCES orders (id),
    showtime_id    BIGINT      NOT NULL REFERENCES showtimes (id),
    seat_id        BIGINT      NOT NULL REFERENCES seats (id),
    seat_label     VARCHAR(10) NOT NULL,
    code           VARCHAR(20) NOT NULL,
    holder_user_id BIGINT      REFERENCES users (id),
    holder_name    VARCHAR(100),
    status         VARCHAR(20) NOT NULL,
    checked_in_at  TIMESTAMPTZ,
    checked_in_by  BIGINT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tickets_code UNIQUE (code),
    CONSTRAINT ck_tickets_status CHECK (status IN ('VALID', 'USED', 'CANCELLED'))
);
-- Chốt chặn cuối của BR01: một ghế trong một suất chỉ có một vé còn hiệu lực
CREATE UNIQUE INDEX ux_tickets_showtime_seat ON tickets (showtime_id, seat_id) WHERE status <> 'CANCELLED';
CREATE INDEX ix_tickets_order ON tickets (order_id);

-- Mỗi giao dịch của cổng thanh toán chỉ được ghi nhận một lần (BR05)
CREATE TABLE payments (
    id              BIGSERIAL    PRIMARY KEY,
    order_id        BIGINT       NOT NULL REFERENCES orders (id),
    provider        VARCHAR(20)  NOT NULL,
    provider_txn_id VARCHAR(100) NOT NULL,
    amount          BIGINT       NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    raw             JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_payments_txn UNIQUE (provider, provider_txn_id),
    CONSTRAINT ck_payments_status CHECK (status IN ('SUCCEEDED', 'FAILED', 'AMOUNT_MISMATCH'))
);
CREATE INDEX ix_payments_order ON payments (order_id);

-- Lịch sử thay đổi đơn (BR09, BKGADM10)
CREATE TABLE order_events (
    id          BIGSERIAL   PRIMARY KEY,
    order_id    BIGINT      NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    type        VARCHAR(40) NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20),
    actor_id    BIGINT,
    data        JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_order_events_order ON order_events (order_id, created_at);

ALTER TABLE showtime_seats
    ADD CONSTRAINT fk_showtime_seats_hold FOREIGN KEY (hold_id) REFERENCES seat_holds (id),
    ADD CONSTRAINT fk_showtime_seats_order FOREIGN KEY (order_id) REFERENCES orders (id);
CREATE INDEX ix_showtime_seats_hold ON showtime_seats (hold_id) WHERE hold_id IS NOT NULL;

CREATE TABLE notifications (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type       VARCHAR(40)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       VARCHAR(1000),
    link       VARCHAR(300),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_notifications_user ON notifications (user_id, created_at DESC);
CREATE INDEX ix_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;
