-- G1: SEO enhancements

-- Add SEO fields to article
ALTER TABLE article ADD COLUMN seo_title VARCHAR(200) AFTER title;
ALTER TABLE article ADD COLUMN meta_description VARCHAR(500) AFTER seo_title;
ALTER TABLE article ADD COLUMN canonical_url VARCHAR(500) AFTER meta_description;
ALTER TABLE article ADD COLUMN og_image VARCHAR(500) AFTER canonical_url;

-- URL redirect table for 301 redirects
CREATE TABLE IF NOT EXISTS url_redirect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    old_path VARCHAR(500) NOT NULL,
    new_path VARCHAR(500) NOT NULL,
    status_code INT DEFAULT 301,
    is_active TINYINT(1) DEFAULT 1,
    hit_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_old_path (old_path(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Site SEO settings
CREATE TABLE IF NOT EXISTS site_seo_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    UNIQUE KEY uk_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default SEO config
INSERT IGNORE INTO site_seo_config (config_key, config_value) VALUES
('site_title', 'My Blog'),
('site_description', 'A personal blog about technology, design, and life'),
('site_url', 'http://localhost:5173'),
('site_author', 'Blog Author'),
('site_language', 'zh-CN'),
('twitter_handle', '@myblog'),
('og_default_image', '/images/og-default.png'),
('robots_custom_rules', '');
