-- V3: Add scheduled_at column to article table
ALTER TABLE article ADD COLUMN scheduled_at DATETIME NULL AFTER published_at;
