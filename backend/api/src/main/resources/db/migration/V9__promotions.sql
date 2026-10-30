-- M8: mã giảm giá (PROADM, BKG05)

CREATE TABLE promotions (
    id             BIGSERIAL    PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL UNIQUE,
    name           VARCHAR(120) NOT NULL,
    description    VARCHAR(500),
    type           VARCHAR(10)  NOT NULL,
    value          BIGINT       NOT NULL,
    max_discount   BIGINT,
    min_order      BIGINT       NOT NULL DEFAULT 0,
    -- Giảm theo số vé (ưu đãi nhóm): đơn phải có ít nhất chừng này vé
    min_tickets    INT          NOT NULL DEFAULT 1,
    starts_at      TIMESTAMPTZ  NOT NULL,
    ends_at        TIMESTAMPTZ  NOT NULL,
    usage_limit    INT,
    per_user_limit INT,
    used_count     INT          NOT NULL DEFAULT 0,
    status         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_promotions_type CHECK (type IN ('PERCENT', 'AMOUNT')),
    CONSTRAINT ck_promotions_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_promotions_value CHECK (value > 0 AND (type <> 'PERCENT' OR value <= 100)),
    CONSTRAINT ck_promotions_period CHECK (starts_at < ends_at),
    CONSTRAINT ck_promotions_limits CHECK (min_order >= 0 AND min_tickets >= 1
        AND (usage_limit IS NULL OR usage_limit > 0) AND (per_user_limit IS NULL OR per_user_limit > 0)
        AND (max_discount IS NULL OR max_discount > 0)),
    -- Chốt chặn cuối cho giới hạn lượt dùng khi nhiều đơn áp mã cùng lúc
    CONSTRAINT ck_promotions_usage CHECK (usage_limit IS NULL OR used_count <= usage_limit)
);

-- Lượt dùng: tạo khi áp mã vào đơn chờ thanh toán, xóa khi đơn hủy/hết hạn (trả lượt)
CREATE TABLE promotion_usages (
    id           BIGSERIAL   PRIMARY KEY,
    promotion_id BIGINT      NOT NULL REFERENCES promotions (id),
    order_id     BIGINT      NOT NULL UNIQUE REFERENCES orders (id),
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    discount     BIGINT      NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_promotion_usages_user ON promotion_usages (promotion_id, user_id);

ALTER TABLE orders ADD COLUMN promotion_code VARCHAR(30);

-- Báo cáo (ADM01/ADM03): lọc đơn theo thời điểm thanh toán
CREATE INDEX ix_orders_paid_at ON orders (paid_at) WHERE paid_at IS NOT NULL;
CREATE INDEX ix_booking_groups_created ON booking_groups (created_at);
