-- E1: Comment System Enhancements

ALTER TABLE comment ADD COLUMN root_id BIGINT DEFAULT NULL AFTER parent_id;
ALTER TABLE comment ADD COLUMN is_pinned TINYINT(1) DEFAULT 0 AFTER like_count;
ALTER TABLE comment ADD INDEX idx_pinned (article_id, is_pinned DESC, created_at DESC);
