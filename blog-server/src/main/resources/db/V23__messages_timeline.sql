CREATE TABLE IF NOT EXISTS `contact_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `message` TEXT NOT NULL,
  `status` VARCHAR(20) DEFAULT 'PENDING',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `reply_content` TEXT,
  `replied_at` DATETIME,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `experience_timeline` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `year` VARCHAR(20) NOT NULL,
  `title` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `sort_order` INT DEFAULT 0,
  `is_visible` BOOLEAN DEFAULT TRUE,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default timeline
INSERT INTO `experience_timeline` (`year`, `title`, `description`, `sort_order`, `is_visible`) VALUES
('2024', '开始写博客', '启动个人博客，分享知识，记录成长旅程。', 10, TRUE),
('2023', '全栈开发', '使用现代框架构建可扩展的 Web 应用。', 20, TRUE),
('2022', '开源贡献', '开始为开源项目贡献代码，并构建开发工具。', 30, TRUE),
('2021', '学习之旅', '深入学习计算机科学基础与软件工程。', 40, TRUE);
