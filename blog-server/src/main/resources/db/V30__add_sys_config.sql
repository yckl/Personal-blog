CREATE TABLE IF NOT EXISTS sys_config (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value LONGTEXT,
    config_name VARCHAR(100) NOT NULL,
    config_type VARCHAR(50) DEFAULT 'STRING',  -- STRING, BOOLEAN, JSON
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO sys_config (config_key, config_value, config_name, config_type, remark) VALUES
('site_name', 'My Personal Blog', '站点名称', 'STRING', '显示在浏览器标题栏尾部的默认名称'),
('allow_comments', 'true', '允许全站留言', 'BOOLEAN', '全局留言板开关');
