-- J1: Membership system + J2: Payment system
-- Drop old stub tables from schema.sql (different schema)
DROP TABLE IF EXISTS member_user;
DROP TABLE IF EXISTS payment_order;
DROP TABLE IF EXISTS membership_plan;

-- Member table (front-end users)
CREATE TABLE IF NOT EXISTS member (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(200) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    nickname        VARCHAR(100),
    avatar          VARCHAR(500),
    tier            VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    tier_expires_at DATETIME,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    login_provider  VARCHAR(30)  DEFAULT 'email',
    provider_id     VARCHAR(200),
    last_login_at   DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_tier (tier),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Membership plans (new schema with tier, price_cents, etc.)
CREATE TABLE membership_plan (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    tier            VARCHAR(20)  NOT NULL,
    duration_months INT          NOT NULL DEFAULT 1,
    price_cents     INT          NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'USD',
    features        TEXT,
    is_active       TINYINT      NOT NULL DEFAULT 1,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Payment orders (new schema with order_no, member_id, amount_cents, etc.)
CREATE TABLE payment_order (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(64)  NOT NULL UNIQUE,
    member_id       BIGINT       NOT NULL,
    plan_id         BIGINT,
    amount_cents    INT          NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'USD',
    payment_method  VARCHAR(20),
    payment_provider VARCHAR(20),
    provider_order_id VARCHAR(200),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    paid_at         DATETIME,
    refunded_at     DATETIME,
    expires_at      DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member (member_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Article visibility: skip if column already exists
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'article' AND column_name = 'visibility');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE article ADD COLUMN visibility VARCHAR(20) DEFAULT ''PUBLIC'' AFTER status', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Default membership plans
INSERT IGNORE INTO membership_plan (name, tier, duration_months, price_cents, currency, features, sort_order) VALUES
('Free', 'FREE', 0, 0, 'USD', 'Access to free articles, comment, subscribe to newsletter', 0),
('Monthly Premium', 'PREMIUM', 1, 999, 'USD', 'All free features, premium articles, early access, no ads', 1),
('Yearly Premium', 'PREMIUM', 12, 7999, 'USD', 'All premium features, 33% discount vs monthly', 2);
