-- M7: hoàn tiền (BR10, BR17, ORDADM-R) và check-in (CHK01)

CREATE TABLE refunds (
    id           BIGSERIAL    PRIMARY KEY,
    order_id     BIGINT       NOT NULL UNIQUE REFERENCES orders (id),
    payment_id   BIGINT       REFERENCES payments (id),
    provider     VARCHAR(20),
    amount       BIGINT       NOT NULL,
    reason       VARCHAR(40)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    provider_ref VARCHAR(100),
    requested_by BIGINT,
    processed_by BIGINT,
    note         VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    CONSTRAINT ck_refunds_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_refunds_amount CHECK (amount >= 0)
);
CREATE INDEX ix_refunds_status ON refunds (status, created_at);

ALTER TABLE tickets ADD CONSTRAINT fk_tickets_checked_in_by FOREIGN KEY (checked_in_by) REFERENCES users (id);
CREATE INDEX ix_orders_showtime_status ON orders (showtime_id, status);
