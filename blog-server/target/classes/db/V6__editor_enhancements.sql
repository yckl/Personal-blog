-- V6: Add tocJson and readingTime to article_content
ALTER TABLE article_content ADD COLUMN toc_json TEXT NULL AFTER content_html;
ALTER TABLE article_content ADD COLUMN reading_time INT NULL DEFAULT 0 AFTER toc_json;
