-- V4: Enhance media_file table with thumbnail, webp, dimensions, and alt text
ALTER TABLE media_file ADD COLUMN thumbnail_url VARCHAR(500) NULL AFTER file_url;
ALTER TABLE media_file ADD COLUMN webp_url VARCHAR(500) NULL AFTER thumbnail_url;
ALTER TABLE media_file ADD COLUMN width INT NULL AFTER webp_url;
ALTER TABLE media_file ADD COLUMN height INT NULL AFTER width;
ALTER TABLE media_file ADD COLUMN alt_text VARCHAR(500) NULL AFTER height;
