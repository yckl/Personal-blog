-- D2: Recommendation System Tables

-- Article similarity scores (precomputed)
CREATE TABLE IF NOT EXISTS article_similarity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    similar_article_id BIGINT NOT NULL,
    score DOUBLE DEFAULT 0,
    reason VARCHAR(50) COMMENT 'TAG_OVERLAP, CATEGORY_MATCH, CONTENT, SERIES',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pair (article_id, similar_article_id),
    INDEX idx_article (article_id),
    INDEX idx_score (article_id, score DESC)
);

-- Recommendation cache (precomputed per article per scene)
CREATE TABLE IF NOT EXISTS article_recommendation_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id BIGINT NOT NULL COMMENT 'article_id or 0 for homepage',
    scene VARCHAR(30) NOT NULL COMMENT 'DETAIL, HOME, SEARCH_EMPTY, NOT_FOUND',
    recommended_ids TEXT NOT NULL COMMENT 'JSON array of article IDs',
    scores TEXT COMMENT 'JSON array of scores',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expired_at DATETIME,
    INDEX idx_source_scene (source_id, scene)
);

-- Exposure and click tracking for feedback loop
CREATE TABLE IF NOT EXISTS recommend_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    recommended_article_id BIGINT NOT NULL,
    scene VARCHAR(30) NOT NULL,
    event_type VARCHAR(20) NOT NULL COMMENT 'EXPOSURE, CLICK',
    user_ip VARCHAR(50),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article (article_id),
    INDEX idx_event (event_type, created_at)
);

-- Manual boost / editorial recommendation
CREATE TABLE IF NOT EXISTS article_manual_boost (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    boost_level INT DEFAULT 0 COMMENT '0=normal, 1=recommend, 2=strong, 3=must',
    target_scene VARCHAR(30) DEFAULT 'ALL' COMMENT 'ALL, DETAIL, HOME',
    start_at DATETIME,
    end_at DATETIME,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article (article_id),
    INDEX idx_active (start_at, end_at)
);
