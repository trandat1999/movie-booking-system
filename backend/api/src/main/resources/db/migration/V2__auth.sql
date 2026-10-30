-- M1: tài khoản, vai trò, quyền, refresh token, nhật ký thao tác

CREATE TABLE users (
    id                BIGSERIAL    PRIMARY KEY,
    email             VARCHAR(255) NOT NULL,
    phone             VARCHAR(20),
    password_hash     VARCHAR(100) NOT NULL,
    full_name         VARCHAR(100) NOT NULL,
    date_of_birth     DATE,
    avatar_url        VARCHAR(500),
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMPTZ,
    last_login_at     TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED'))
);

CREATE TABLE roles (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT uq_roles_code UNIQUE (code)
);

CREATE TABLE permissions (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    group_name  VARCHAR(50)  NOT NULL,
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

-- Refresh token chỉ lưu hash SHA-256; family_id gom các token sinh ra từ cùng một lần đăng nhập
-- để thu hồi cả chuỗi khi phát hiện token cũ bị dùng lại.
CREATE TABLE refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    family_id   UUID         NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    revoke_reason VARCHAR(20),
    user_agent  VARCHAR(255),
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);

CREATE TABLE audit_logs (
    id           BIGSERIAL    PRIMARY KEY,
    actor_id     BIGINT,
    actor_email  VARCHAR(255),
    action       VARCHAR(50)  NOT NULL,
    resource     VARCHAR(50)  NOT NULL,
    resource_id  VARCHAR(50),
    data         JSONB,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX ix_audit_logs_resource ON audit_logs (resource, resource_id);
CREATE INDEX ix_audit_logs_actor ON audit_logs (actor_id);

-- Dữ liệu danh mục vai trò & quyền (dùng ở mọi môi trường)
INSERT INTO roles (code, name, description) VALUES
    ('ADMIN',    'Quản trị viên', 'Toàn quyền quản trị hệ thống'),
    ('STAFF',    'Nhân viên rạp', 'Kiểm tra vé và tra cứu đơn tại rạp'),
    ('CUSTOMER', 'Khách hàng',    'Người dùng đặt vé');

INSERT INTO permissions (code, name, group_name) VALUES
    ('admin:access',    'Truy cập trang quản trị',        'Hệ thống'),
    ('user:read',       'Xem người dùng',                 'Người dùng'),
    ('user:write',      'Khóa/mở khóa, phân vai trò',     'Người dùng'),
    ('role:read',       'Xem vai trò và quyền',           'Người dùng'),
    ('audit:read',      'Xem nhật ký thao tác',           'Hệ thống'),
    ('setting:write',   'Cấu hình hệ thống',              'Hệ thống'),
    ('movie:write',     'Quản lý phim',                   'Danh mục'),
    ('cinema:write',    'Quản lý rạp, phòng, sơ đồ ghế',  'Danh mục'),
    ('showtime:write',  'Quản lý lịch chiếu và giá vé',   'Danh mục'),
    ('order:read',      'Xem đơn đặt vé',                 'Bán vé'),
    ('order:refund',    'Hủy vé và hoàn tiền',            'Bán vé'),
    ('ticket:checkin',  'Kiểm tra vé tại rạp',            'Bán vé'),
    ('group:read',      'Xem nhóm đặt vé',                'Bán vé'),
    ('promotion:write', 'Quản lý khuyến mãi',             'Bán vé'),
    ('stats:read',      'Xem thống kê, báo cáo',          'Báo cáo');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
    ON p.code IN ('admin:access', 'order:read', 'ticket:checkin', 'group:read')
WHERE r.code = 'STAFF';
