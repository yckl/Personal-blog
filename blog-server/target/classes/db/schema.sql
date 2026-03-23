-- =====================================================
-- Personal Blog System - Database Schema (MySQL 8.x)
-- =====================================================

-- 1. User & Role
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    nickname      VARCHAR(100),
    avatar        VARCHAR(500),
    email         VARCHAR(200),
    bio           TEXT,
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1=active, 0=disabled',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_role (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_name     VARCHAR(50)  NOT NULL UNIQUE,
    role_key      VARCHAR(50)  NOT NULL UNIQUE,
    description   VARCHAR(255),
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sys_user_role (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    role_id       BIGINT       NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Article
CREATE TABLE article_category (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(100) NOT NULL UNIQUE,
    description   TEXT,
    parent_id     BIGINT       DEFAULT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_tag (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(100) NOT NULL UNIQUE,
    color         VARCHAR(20),
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(300) NOT NULL,
    slug          VARCHAR(300) NOT NULL UNIQUE,
    excerpt       TEXT,
    cover_image   VARCHAR(500),
    author_id     BIGINT       NOT NULL,
    category_id   BIGINT       DEFAULT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, PUBLISHED, ARCHIVED',
    is_top        TINYINT(1)   NOT NULL DEFAULT 0,
    is_featured   TINYINT(1)   NOT NULL DEFAULT 0,
    allow_comment TINYINT(1)   NOT NULL DEFAULT 1,
    view_count    INT          NOT NULL DEFAULT 0,
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    word_count    INT          NOT NULL DEFAULT 0,
    published_at  DATETIME     DEFAULT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_author (author_id),
    KEY idx_category (category_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_content (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT       NOT NULL UNIQUE,
    content_md    LONGTEXT,
    content_html  LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_tag_rel (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT       NOT NULL,
    tag_id        BIGINT       NOT NULL,
    UNIQUE KEY uk_article_tag (article_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_series (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    slug          VARCHAR(200) NOT NULL UNIQUE,
    description   TEXT,
    cover_image   VARCHAR(500),
    sort_order    INT          NOT NULL DEFAULT 0,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_series_rel (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT       NOT NULL,
    series_id     BIGINT       NOT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_article_series (article_id, series_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_version (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT       NOT NULL,
    title         VARCHAR(300),
    content_md    LONGTEXT,
    version_num   INT          NOT NULL DEFAULT 1,
    created_by    BIGINT       DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Comments
CREATE TABLE comment (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT       NOT NULL,
    parent_id     BIGINT       DEFAULT NULL,
    author_name   VARCHAR(100) NOT NULL,
    author_email  VARCHAR(200),
    author_url    VARCHAR(500),
    author_avatar VARCHAR(500),
    content       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    ip_address    VARCHAR(50),
    user_agent    VARCHAR(500),
    like_count    INT          NOT NULL DEFAULT 0,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_article (article_id),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment_like (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    comment_id    BIGINT       NOT NULL,
    ip_address    VARCHAR(50)  NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_comment_ip (comment_id, ip_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Engagement
CREATE TABLE article_like (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT       NOT NULL,
    ip_address    VARCHAR(50)  NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_ip (article_id, ip_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE article_favorite (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_user (article_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Media
CREATE TABLE media_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    original_name VARCHAR(500) NOT NULL,
    stored_name   VARCHAR(500) NOT NULL,
    file_path     VARCHAR(1000) NOT NULL,
    file_url      VARCHAR(1000) NOT NULL,
    file_size     BIGINT       NOT NULL DEFAULT 0,
    file_type     VARCHAR(100),
    mime_type     VARCHAR(100),
    uploaded_by   BIGINT       DEFAULT NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Site Config
CREATE TABLE site_config (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    config_key    VARCHAR(200) NOT NULL UNIQUE,
    config_value  TEXT,
    description   VARCHAR(500),
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE menu_config (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    url           VARCHAR(500),
    icon          VARCHAR(100),
    parent_id     BIGINT       DEFAULT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    is_external   TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Subscriber / Newsletter
CREATE TABLE subscriber (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(200) NOT NULL UNIQUE,
    name            VARCHAR(100),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, UNSUBSCRIBED',
    subscribed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at DATETIME     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE newsletter_task (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    subject       VARCHAR(500) NOT NULL,
    content       TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, SENDING, SENT, FAILED',
    total_count   INT          NOT NULL DEFAULT 0,
    sent_count    INT          NOT NULL DEFAULT 0,
    scheduled_at  DATETIME     DEFAULT NULL,
    sent_at       DATETIME     DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE newsletter_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id       BIGINT       NOT NULL,
    subscriber_id BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_msg     TEXT,
    sent_at       DATETIME     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Analytics
CREATE TABLE visit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT       DEFAULT NULL,
    page_url      VARCHAR(1000),
    ip_address    VARCHAR(50),
    user_agent    VARCHAR(500),
    referer       VARCHAR(1000),
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE search_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    keyword       VARCHAR(200) NOT NULL,
    result_count  INT          NOT NULL DEFAULT 0,
    ip_address    VARCHAR(50),
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Notifications
CREATE TABLE notification_task (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type          VARCHAR(50)  NOT NULL,
    title         VARCHAR(300),
    content       TEXT,
    target_user   BIGINT       DEFAULT NULL,
    is_read       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Membership & Payment (stubs)
CREATE TABLE membership_plan (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    price         DECIMAL(10,2) NOT NULL DEFAULT 0,
    duration_days INT          NOT NULL DEFAULT 30,
    status        TINYINT      NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE member_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    plan_id       BIGINT       NOT NULL,
    start_date    DATETIME     NOT NULL,
    end_date      DATETIME     NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment_order (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_no       VARCHAR(100)  NOT NULL UNIQUE,
    user_id        BIGINT        NOT NULL,
    plan_id        BIGINT        DEFAULT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    paid_at        DATETIME      DEFAULT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. System
CREATE TABLE system_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       DEFAULT NULL,
    operation     VARCHAR(200),
    method        VARCHAR(20),
    params        TEXT,
    ip_address    VARCHAR(50),
    duration      BIGINT,
    status        TINYINT      NOT NULL DEFAULT 1,
    error_msg     TEXT,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE import_export_task (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type          VARCHAR(20)  NOT NULL COMMENT 'IMPORT, EXPORT',
    format        VARCHAR(20)  NOT NULL COMMENT 'MARKDOWN, JSON, XML',
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    file_path     VARCHAR(1000),
    total_count   INT          NOT NULL DEFAULT 0,
    success_count INT          NOT NULL DEFAULT 0,
    error_msg     TEXT,
    created_by    BIGINT       DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at  DATETIME     DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default roles
INSERT INTO sys_role (role_name, role_key, description) VALUES
('Administrator', 'ADMIN', 'Full system access'),
('Editor', 'EDITOR', 'Can manage all content'),
('Author', 'AUTHOR', 'Can manage own content');

-- Insert default site configs
INSERT INTO site_config (config_key, config_value, description) VALUES
('site_title', 'My Blog', 'Site title'),
('site_description', 'A personal blog about technology and life', 'Site description'),
('site_keywords', 'blog,technology,programming', 'SEO keywords'),
('site_author', 'Blog Author', 'Author name'),
('site_logo', '', 'Site logo URL'),
('site_favicon', '', 'Favicon URL'),
('site_footer', '© 2026 My Blog. All rights reserved.', 'Footer text'),
('comment_review', 'true', 'Whether comments need review'),
('site_icp', '', 'ICP备案号'),
('social_github', '', 'GitHub URL'),
('social_twitter', '', 'Twitter URL'),
('social_email', '', 'Contact email');
