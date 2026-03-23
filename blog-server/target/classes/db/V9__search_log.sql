-- Search log table for analytics
CREATE TABLE IF NOT EXISTS search_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(200) NOT NULL,
    result_count INT DEFAULT 0,
    user_ip VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_keyword (keyword),
    INDEX idx_created (created_at)
);
