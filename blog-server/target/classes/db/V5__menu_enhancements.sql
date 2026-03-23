-- V5: Enhance menu_config with visible and target fields
ALTER TABLE menu_config ADD COLUMN visible TINYINT(1) NOT NULL DEFAULT 1 AFTER sort_order;
ALTER TABLE menu_config ADD COLUMN target VARCHAR(20) NULL DEFAULT '_self' AFTER url;
