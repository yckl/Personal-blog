-- V7: AI task log for auditing AI-generated content
CREATE TABLE IF NOT EXISTS ai_task_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NULL,
    task_type VARCHAR(50) NOT NULL COMMENT 'title, summary, seo_title, meta_description, outline, tags, newsletter, internal_links',
    input_text TEXT,
    result_json TEXT,
    model VARCHAR(100),
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article_id (article_id),
    INDEX idx_task_type (task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
