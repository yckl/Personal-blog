-- Friend Link table
CREATE TABLE IF NOT EXISTS `friend_link` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT 'Link name',
    `url` VARCHAR(500) NOT NULL COMMENT 'Link URL',
    `logo` VARCHAR(500) DEFAULT NULL COMMENT 'Logo image URL',
    `description` VARCHAR(500) DEFAULT NULL COMMENT 'Short description',
    `sort_order` INT DEFAULT 0 COMMENT 'Display order',
    `status` TINYINT DEFAULT 1 COMMENT '1=active, 0=hidden',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Friend links';
