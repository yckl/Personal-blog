-- V21: Push subscriptions, A/B testing, multi-author

-- Push notification subscriptions
CREATE TABLE IF NOT EXISTS push_subscription (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    endpoint        TEXT         NOT NULL,
    p256dh_key      VARCHAR(500),
    auth_key        VARCHAR(500),
    member_id       BIGINT,
    visitor_id      VARCHAR(64),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_member (member_id),
    INDEX idx_visitor (visitor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- A/B test experiments
CREATE TABLE IF NOT EXISTS ab_experiment (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    start_date      DATETIME,
    end_date        DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- A/B test variants
CREATE TABLE IF NOT EXISTS ab_variant (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    experiment_id   BIGINT       NOT NULL,
    name            VARCHAR(50)  NOT NULL,
    config_json     TEXT,
    traffic_percent INT          NOT NULL DEFAULT 50,
    views           INT          NOT NULL DEFAULT 0,
    conversions     INT          NOT NULL DEFAULT 0,
    INDEX idx_experiment (experiment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- A/B visitor assignments
CREATE TABLE IF NOT EXISTS ab_assignment (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    experiment_id   BIGINT       NOT NULL,
    variant_id      BIGINT       NOT NULL,
    visitor_id      VARCHAR(64)  NOT NULL,
    converted       TINYINT      NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_exp_visitor (experiment_id, visitor_id),
    INDEX idx_variant (variant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Multi-author: add author profile fields to sys_user + article co-author
SET @col_author_bio = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'author_bio');
SET @sql1 = IF(@col_author_bio = 0, 'ALTER TABLE sys_user ADD COLUMN author_bio TEXT, ADD COLUMN social_links TEXT, ADD COLUMN is_author TINYINT DEFAULT 0', 'SELECT 1');
PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- Article co-authors
CREATE TABLE IF NOT EXISTS article_author (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id      BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'CO_AUTHOR',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_user (article_id, user_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
