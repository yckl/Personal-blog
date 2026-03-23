-- F2: Newsletter enhancements + F3: Welcome sequence
-- NOTE: Uses conditional column additions to handle partial migration recovery

-- Safe column adds for newsletter_task (ignores if already exists)
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_task' AND COLUMN_NAME = 'article_id') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_task ADD COLUMN article_id BIGINT AFTER id'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_task' AND COLUMN_NAME = 'content_html') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_task ADD COLUMN content_html MEDIUMTEXT AFTER content'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_task' AND COLUMN_NAME = 'audience_type') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_task ADD COLUMN audience_type VARCHAR(30) DEFAULT ''ALL'' AFTER content_html'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_task' AND COLUMN_NAME = 'failed_count') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_task ADD COLUMN failed_count INT DEFAULT 0 AFTER sent_count'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_task' AND COLUMN_NAME = 'created_by') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_task ADD COLUMN created_by BIGINT AFTER scheduled_at'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Safe column adds for newsletter_log
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_log' AND COLUMN_NAME = 'opened') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_log ADD COLUMN opened TINYINT(1) DEFAULT 0 AFTER error_msg'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_log' AND COLUMN_NAME = 'opened_at') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_log ADD COLUMN opened_at DATETIME AFTER opened'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_log' AND COLUMN_NAME = 'clicked') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_log ADD COLUMN clicked TINYINT(1) DEFAULT 0 AFTER opened_at'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_log' AND COLUMN_NAME = 'clicked_at') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_log ADD COLUMN clicked_at DATETIME AFTER clicked'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Indexes (safe — MySQL ignores duplicate index names in CREATE INDEX IF NOT EXISTS isn't available, so we use a check)
SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_log' AND INDEX_NAME = 'idx_task') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_log ADD INDEX idx_task (task_id)'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'newsletter_log' AND INDEX_NAME = 'idx_subscriber') > 0,
    'SELECT 1',
    'ALTER TABLE newsletter_log ADD INDEX idx_subscriber (subscriber_id)'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- F3: Welcome email sequence
CREATE TABLE IF NOT EXISTS welcome_sequence_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    step_order INT NOT NULL COMMENT '1=welcome, 2=day3, 3=day7',
    delay_days INT NOT NULL DEFAULT 0,
    subject VARCHAR(500) NOT NULL,
    content_html MEDIUMTEXT NOT NULL,
    is_active TINYINT(1) DEFAULT 1,
    recommend_article_ids VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_step (step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS welcome_sequence_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    scheduled_at DATETIME NOT NULL,
    sent_at DATETIME,
    error_msg TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_scheduled (status, scheduled_at),
    INDEX idx_subscriber (subscriber_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default welcome templates (ignore if already exist)
INSERT IGNORE INTO welcome_sequence_template (step_order, delay_days, subject, content_html) VALUES
(1, 0, 'Welcome to our blog!', '<!DOCTYPE html><html><head><meta charset="utf-8"></head><body style="margin:0;padding:0;background:#0f0f23;font-family:-apple-system,sans-serif;"><div style="max-width:600px;margin:0 auto;padding:32px 24px;"><h1 style="color:#fff;font-size:28px;">Welcome aboard!</h1><p style="color:#a0a0b0;font-size:16px;line-height:1.8;">Thank you for subscribing! We are excited to have you join our community.</p><a href="{{BASE_URL}}" style="display:inline-block;margin-top:16px;padding:12px 28px;background:#6366f1;color:#fff;text-decoration:none;border-radius:8px;font-weight:600;">Visit the Blog</a><hr style="border:1px solid #1e1e3a;margin:32px 0;"><p style="color:#555;font-size:12px;"><a href="{{BASE_URL}}/unsubscribe?token={{UNSUB_TOKEN}}" style="color:#6366f1;">Unsubscribe</a></p></div></body></html>'),
(2, 3, 'Our best articles, handpicked for you', '<!DOCTYPE html><html><head><meta charset="utf-8"></head><body style="margin:0;padding:0;background:#0f0f23;font-family:-apple-system,sans-serif;"><div style="max-width:600px;margin:0 auto;padding:32px 24px;"><h1 style="color:#fff;font-size:24px;">Our Best Reads</h1><p style="color:#a0a0b0;font-size:16px;line-height:1.8;">Here are some popular articles we think you will love:</p><p style="color:#a0a0b0;font-size:16px;">{{RECOMMENDED_ARTICLES}}</p><a href="{{BASE_URL}}/articles" style="display:inline-block;margin-top:16px;padding:12px 28px;background:#6366f1;color:#fff;text-decoration:none;border-radius:8px;font-weight:600;">Browse All Articles</a><hr style="border:1px solid #1e1e3a;margin:32px 0;"><p style="color:#555;font-size:12px;"><a href="{{BASE_URL}}/unsubscribe?token={{UNSUB_TOKEN}}" style="color:#6366f1;">Unsubscribe</a></p></div></body></html>'),
(3, 7, 'A week in! Here is what is coming next', '<!DOCTYPE html><html><head><meta charset="utf-8"></head><body style="margin:0;padding:0;background:#0f0f23;font-family:-apple-system,sans-serif;"><div style="max-width:600px;margin:0 auto;padding:32px 24px;"><h1 style="color:#fff;font-size:24px;">One week in!</h1><p style="color:#a0a0b0;font-size:16px;line-height:1.8;">It has been a week since you joined us. Stay tuned for exciting articles and series coming up.</p><a href="{{BASE_URL}}/archive" style="display:inline-block;margin-top:16px;padding:12px 28px;background:#6366f1;color:#fff;text-decoration:none;border-radius:8px;font-weight:600;">Explore Archive</a><hr style="border:1px solid #1e1e3a;margin:32px 0;"><p style="color:#555;font-size:12px;"><a href="{{BASE_URL}}/unsubscribe?token={{UNSUB_TOKEN}}" style="color:#6366f1;">Unsubscribe</a></p></div></body></html>');
