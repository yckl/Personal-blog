-- E2: Engagement enhancements + E3: Notes V2 schema

-- Article share tracking (device/IP-based)
CREATE TABLE IF NOT EXISTS article_share (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    platform VARCHAR(30) NOT NULL COMMENT 'twitter, linkedin, wechat, copy_link',
    ip_address VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article (article_id),
    INDEX idx_platform (platform)
);

-- E3: Notes / Short Updates (V2 lightweight)
CREATE TABLE IF NOT EXISTS post_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL,
    note_type VARCHAR(20) DEFAULT 'TEXT' COMMENT 'TEXT, IMAGE, QUOTE, LINK, POLL',
    media_url VARCHAR(1000),
    link_url VARCHAR(1000),
    link_title VARCHAR(300),
    like_count INT DEFAULT 0,
    reply_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PUBLISHED',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status, created_at DESC)
);

CREATE TABLE IF NOT EXISTS note_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_note_ip (note_id, ip_address)
);

CREATE TABLE IF NOT EXISTS note_reply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    parent_id BIGINT,
    author_name VARCHAR(100) NOT NULL,
    author_email VARCHAR(200),
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'APPROVED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_note (note_id)
);
