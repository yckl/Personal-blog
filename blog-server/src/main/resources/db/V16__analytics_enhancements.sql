-- H1: Analytics enhancements

-- Add visitor tracking, page type, geo, and stay duration to visit_log
ALTER TABLE visit_log ADD COLUMN visitor_id VARCHAR(64) AFTER page_url;
ALTER TABLE visit_log ADD COLUMN page_type VARCHAR(30) DEFAULT 'article' AFTER visitor_id;
ALTER TABLE visit_log ADD COLUMN country VARCHAR(50) AFTER referer;
ALTER TABLE visit_log ADD COLUMN city VARCHAR(100) AFTER country;
ALTER TABLE visit_log ADD COLUMN device_type VARCHAR(20) AFTER city;
ALTER TABLE visit_log ADD COLUMN browser VARCHAR(50) AFTER device_type;
ALTER TABLE visit_log ADD COLUMN os VARCHAR(50) AFTER browser;
ALTER TABLE visit_log ADD COLUMN stay_seconds INT DEFAULT 0 AFTER os;

ALTER TABLE visit_log ADD INDEX idx_visitor (visitor_id);
ALTER TABLE visit_log ADD INDEX idx_created (created_at);
ALTER TABLE visit_log ADD INDEX idx_page_type (page_type);
