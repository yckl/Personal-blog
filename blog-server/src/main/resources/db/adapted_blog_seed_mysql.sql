-- Adapted MySQL seed script for the uploaded schema in personal blog.sql
-- Safe to run multiple times where possible via ON DUPLICATE KEY UPDATE / INSERT IGNORE.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
START TRANSACTION;

-- =========================================================
-- 1) Ensure core roles and an author-capable admin user exist
-- =========================================================
INSERT INTO sys_role (role_name, role_key, description)
VALUES
  ('Super Administrator', 'SUPER_ADMIN', 'Full system access including user management'),
  ('Author', 'AUTHOR', 'Can manage own content'),
  ('Editor', 'EDITOR', 'Can manage all content')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO sys_user (
  username, password, nickname, avatar, email, bio,
  status, two_factor_enabled, two_factor_secret,
  last_login_at, last_login_ip, deleted,
  author_bio, social_links, is_author
)
VALUES (
  'seed_author',
  '$2b$12$x2XE.MxAUs.2.9Z1KJ1Hr.8kq0/YSIKmPAOug/x3Mad/wHAcLpuwW',
  '种子作者',
  NULL,
  'seed_author@blog.com',
  '系统初始化生成的演示作者账号。',
  1, 0, NULL,
  NULL, NULL, 0,
  '一个持续写作、关注技术与内容产品的作者。',
  '{"github":"","x":"","email":"seed_author@blog.com"}',
  1
)
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  email = VALUES(email),
  bio = VALUES(bio),
  author_bio = VALUES(author_bio),
  social_links = VALUES(social_links),
  is_author = VALUES(is_author),
  deleted = 0;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_key = 'SUPER_ADMIN'
WHERE u.username = 'seed_author';

-- =========================================================
-- 2) Categories
-- =========================================================
INSERT INTO article_category (name, slug, description, parent_id, sort_order, deleted)
VALUES
  ('技术', 'tech', '技术开发、架构设计与工程实践', NULL, 1, 0),
  ('项目实战', 'projects', '从 0 到 1 的项目开发实战', NULL, 2, 0),
  ('产品', 'product', '产品设计、需求分析与功能规划', NULL, 3, 0),
  ('设计', 'design', 'UI、UX、设计系统与视觉表达', NULL, 4, 0),
  ('运营', 'growth', 'SEO、订阅、增长与内容分发', NULL, 5, 0),
  ('读书资源', 'resources', '书单、工具与学习资源整理', NULL, 6, 0),
  ('生活', 'life', '个人记录、效率与日常生活', NULL, 7, 0),
  ('随笔', 'essays', '碎片化思考与个人表达', NULL, 8, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  deleted = 0;

-- =========================================================
-- 3) Tags
-- =========================================================
INSERT INTO article_tag (name, slug, color, deleted)
VALUES
  ('Spring Boot', 'spring-boot', NULL, 0),
  ('Spring Security', 'spring-security', NULL, 0),
  ('Java', 'java', NULL, 0),
  ('Vue 3', 'vue-3', NULL, 0),
  ('Vite', 'vite', NULL, 0),
  ('Pinia', 'pinia', NULL, 0),
  ('MyBatis Plus', 'mybatis-plus', NULL, 0),
  ('PostgreSQL', 'postgresql', NULL, 0),
  ('Redis', 'redis', NULL, 0),
  ('Docker', 'docker', NULL, 0),
  ('Nginx', 'nginx', NULL, 0),
  ('JWT', 'jwt', NULL, 0),
  ('REST API', 'rest-api', NULL, 0),
  ('Markdown', 'markdown', NULL, 0),
  ('Elasticsearch', 'elasticsearch', NULL, 0),
  ('推荐系统', 'recommendation-system', NULL, 0),
  ('PRD', 'prd', NULL, 0),
  ('后台管理系统', 'admin-dashboard', NULL, 0),
  ('内容平台', 'content-platform', NULL, 0),
  ('用户增长', 'user-growth', NULL, 0),
  ('Newsletter', 'newsletter', NULL, 0),
  ('数据分析', 'analytics', NULL, 0),
  ('MVP', 'mvp', NULL, 0),
  ('功能设计', 'feature-design', NULL, 0),
  ('UI 设计', 'ui-design', NULL, 0),
  ('交互设计', 'interaction-design', NULL, 0),
  ('暗黑模式', 'dark-mode', NULL, 0),
  ('响应式设计', 'responsive-design', NULL, 0),
  ('可访问性', 'accessibility', NULL, 0),
  ('阅读体验', 'reading-experience', NULL, 0),
  ('系列文章', 'series-articles', NULL, 0),
  ('开发日志', 'dev-log', NULL, 0),
  ('项目复盘', 'project-review', NULL, 0),
  ('SEO', 'seo', NULL, 0),
  ('JSON-LD', 'json-ld', NULL, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  color = VALUES(color),
  deleted = 0;

-- =========================================================
-- 4) Series
-- =========================================================
INSERT INTO article_series (name, slug, description, cover_image, sort_order, deleted)
VALUES
  ('从 0 到 1 做个人博客系统', 'build-a-blog-system', '从需求分析、数据库设计、后台开发到前端实现的完整博客系统实战', NULL, 1, 0),
  ('Spring Boot + Vue 全栈实战', 'springboot-vue-fullstack', '围绕 Spring Boot 与 Vue 的完整项目开发经验总结', NULL, 2, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  deleted = 0;

-- =========================================================
-- 5) Articles (12 complete seed articles)
-- =========================================================
INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  'Spring Boot + Vue 构建个人博客系统的技术选型',
  'Spring Boot + Vue 构建个人博客系统的技术选型',
  '分析个人博客系统技术选型，包括 Spring Boot、Vue 3、PostgreSQL、Redis 和对象存储。',
  NULL, NULL, 'springboot-vue-blog-tech-stack',
  '从后端框架、前端框架、数据库、搜索、存储等多个维度分析个人博客系统的技术选型思路。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  1, 1, 1, 1260, 58,
  12, 1800, '2026-03-01 10:00:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'projects'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE
  title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description),
  excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id),
  status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top),
  is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment),
  view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count),
  word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '博客后台管理系统应该包含哪些核心功能',
  '博客后台管理系统应该包含哪些核心功能',
  '拆解博客后台管理系统的核心模块，包括仪表盘、文章、评论、媒体和站点设置。',
  NULL, NULL, 'blog-admin-dashboard-core-features',
  '从后台作者视角，拆解一个博客管理系统必须具备的核心功能模块和优先级。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 1, 1, 980, 42,
  8, 1500, '2026-03-03 14:00:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'product'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '如何设计文章分类、标签和系列文集',
  '如何设计文章分类、标签和系列文集',
  '讲解博客中分类、标签、系列、归档等内容组织方式的设计思路。',
  NULL, NULL, 'design-blog-category-tag-series',
  '分类、标签、系列、归档分别解决什么问题？这篇文章把博客内容组织体系讲清楚。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 0, 1, 760, 33,
  6, 1300, '2026-03-05 09:30:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'product'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '个人博客系统数据库表设计详解',
  '个人博客系统数据库表设计详解',
  '详细设计博客系统数据库结构，包括用户、文章、评论、订阅和统计模块。',
  NULL, NULL, 'blog-database-schema-design',
  '围绕用户、文章、标签、评论、订阅、统计等模块，详细设计博客系统数据库结构。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 1, 1, 1540, 73,
  15, 2400, '2026-03-07 11:20:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'tech'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '博客推荐系统怎么做得更智能',
  '博客推荐系统怎么做得更智能',
  '讲解博客推荐系统的召回、排序、重排与个性化设计思路。',
  NULL, NULL, 'advanced-blog-recommendation-system',
  '从标签召回、内容相似度、协同过滤到重排策略，完整讲解博客推荐系统如何升级。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 1, 1, 1880, 91,
  19, 2600, '2026-03-09 16:00:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'tech'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '使用 Vue 3 实现博客前台首页',
  '使用 Vue 3 实现博客前台首页',
  '使用 Vue 3 构建博客首页，包含模块拆分、组件设计和响应式实现。',
  NULL, NULL, 'build-blog-homepage-with-vue3',
  '从首页模块拆分、组件设计、响应式布局到数据驱动渲染，讲清楚博客首页如何实现。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 0, 1, 860, 37,
  7, 1600, '2026-03-10 13:00:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'tech'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  'Spring Security + JWT 登录认证实战',
  'Spring Security + JWT 登录认证实战',
  '通过 Spring Security 和 JWT 实现博客后台登录认证与权限控制。',
  NULL, NULL, 'spring-security-jwt-auth-practice',
  '用 Spring Security 和 JWT 实现后台登录、权限控制、Token 刷新与接口鉴权。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 0, 1, 1320, 64,
  10, 2100, '2026-03-12 15:30:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'tech'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  'Markdown 编辑器模块的设计与实现',
  'Markdown 编辑器模块的设计与实现',
  '设计博客后台的 Markdown 编辑器，包括编辑、预览、存储与渲染。',
  NULL, NULL, 'markdown-editor-design-and-implementation',
  '一篇文章讲清楚 Markdown 编辑器在博客后台中的结构设计、存储方式和预览实现。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 0, 1, 940, 41,
  9, 1700, '2026-03-14 09:45:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'tech'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '博客 SEO 优化完整方案',
  '博客 SEO 优化完整方案',
  '完整讲解博客 SEO 优化，包括 Meta、JSON-LD、站点地图与页面性能。',
  NULL, NULL, 'complete-blog-seo-optimization-guide',
  '从标题、Meta、结构化数据、站点地图到页面性能，系统讲解博客 SEO 优化的完整方案。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 1, 1, 1710, 79,
  11, 2200, '2026-03-15 10:15:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'growth'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  'Newsletter 订阅系统设计',
  'Newsletter 订阅系统设计',
  '拆解 Newsletter 订阅系统，包括订阅、确认、发送、分组与统计。',
  NULL, NULL, 'newsletter-subscription-system-design',
  '从订阅入口、确认邮件、欢迎序列、分组发送到统计分析，完整拆解 Newsletter 模块。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 1, 1, 1250, 54,
  8, 1850, '2026-03-17 18:20:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'growth'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '暗黑模式与阅读体验优化',
  '暗黑模式与阅读体验优化',
  '讲解博客中暗黑模式、排版、对比度和可访问性的设计优化。',
  NULL, NULL, 'dark-mode-and-reading-experience',
  '暗黑模式不是简单换个颜色，而是阅读体验、对比度、可访问性和主题系统的综合设计。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 0, 1, 670, 29,
  4, 1200, '2026-03-18 20:00:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'design'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '从 0 开发个人博客系统项目复盘',
  '从 0 开发个人博客系统项目复盘',
  '完整复盘个人博客系统项目，从需求分析到开发上线的经验总结。',
  NULL, NULL, 'blog-system-project-retrospective',
  '回顾个人博客系统从需求、设计、开发到优化的完整过程，总结关键经验和踩坑记录。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  1, 1, 1, 2100, 105,
  22, 2300, '2026-03-20 21:10:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'projects'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '长期写作最难的，从来不是没有时间，而是没有稳定节奏',
  '长期写作最难的，从来不是没有时间，而是没有稳定节奏',
  '从生活与习惯角度讨论长期写作中最重要的是稳定节奏而不是绝对时间。',
  NULL, NULL, 'writing-consistency-matters-more-than-time',
  '很多人觉得写不下去是因为太忙，但长期看，真正让写作中断的往往不是时间不够，而是节奏不稳定、心理门槛太高。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 0, 1, 730, 27,
  2, 1900, '2026-03-21 10:10:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'life'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

INSERT INTO article (
  title, seo_title, meta_description, canonical_url, og_image, slug,
  excerpt, cover_image, author_id, category_id, status, visibility,
  is_top, is_featured, allow_comment, view_count, like_count,
  comment_count, word_count, published_at, scheduled_at, deleted
)
SELECT
  '我越来越不想做“看起来很厉害”的东西了',
  '我越来越不想做看起来很厉害的东西了',
  '一篇关于项目观与成长心态的随笔：比起表面厉害，更重要的是长期留下来的价值。',
  NULL, NULL, 'i-no-longer-chase-impressive-things',
  '随着做的项目越来越多，我越来越发现，真正值得投入的，不是那些第一眼看起来很厉害的东西，而是那些能长期使用、长期积累、长期留下来的东西。',
  NULL,
  u.id,
  c.id,
  'PUBLISHED', 'PUBLIC',
  0, 1, 1, 1120, 49,
  6, 1800, '2026-03-22 10:10:00', NULL, 0
FROM sys_user u JOIN article_category c ON c.slug = 'essays'
WHERE u.username = 'seed_author'
ON DUPLICATE KEY UPDATE title = VALUES(title), seo_title = VALUES(seo_title), meta_description = VALUES(meta_description), excerpt = VALUES(excerpt), author_id = VALUES(author_id), category_id = VALUES(category_id), status = VALUES(status), visibility = VALUES(visibility), is_top = VALUES(is_top), is_featured = VALUES(is_featured), allow_comment = VALUES(allow_comment), view_count = VALUES(view_count), like_count = VALUES(like_count), comment_count = VALUES(comment_count), word_count = VALUES(word_count), published_at = VALUES(published_at), deleted = 0;

-- =========================================================
-- 6) Article content (complete-ish bodies)
-- =========================================================
INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
'# Spring Boot + Vue 构建个人博客系统的技术选型\n\n做个人博客系统，最容易出现的一个误区，就是在“想做一个博客”这件事上不断叠加技术幻想：要不要上微服务，要不要上全栈同构，要不要一步做到会员、支付、社区和推荐。结果通常不是系统更强，而是项目更难落地。\n\n如果把目标拉回到现实，一个博客系统的本质其实非常清晰：它首先是一个内容管理系统，其次是一个阅读体验系统，最后才是一个增长和订阅系统。围绕这个本质去选技术，Spring Boot 加 Vue 之所以合适，不是因为它们最潮，而是因为它们在这个问题上的边界非常清晰。\n\n## 后端为什么选 Spring Boot\n\n博客后端看上去简单，实际上很考验结构设计。文章管理、分类标签、系列、评论、订阅、搜索、权限、媒体上传、SEO 配置、数据统计，这些模块单个看都不复杂，但一旦放在一起，就要求后端有很强的模块化稳定性。Spring Boot 的优势就在这里：它不是为了炫技存在，而是为了让你把系统拆清楚。\n\n## 前端为什么选 Vue\n\nVue 的优势不是能做页面，而是它让页面的组织方式很自然。一个博客前台并不是只有文章详情页，它至少还会有首页、分类页、标签页、归档页、系列页、搜索页和 About 页；后台则需要仪表盘、文章管理、编辑器、评论管理、媒体库和站点设置。\n\n## 技术栈选择的关键\n\n很多人会问，既然是博客，为什么不直接用现成 CMS？答案很简单：如果你只是想拥有一个博客，那直接用现成平台当然更快；但如果你的目标是做一套自己的系统、作为作品或者长期维护的产品，那你需要的是控制力。\n\n## 总结\n\n个人博客系统不是为了证明你会多少框架，而是为了构建一个能写、能看、能搜、能增长、能长期维护的内容产品。',
'<h1>Spring Boot + Vue 构建个人博客系统的技术选型</h1><p>做个人博客系统，最容易出现的一个误区，就是在想做一个博客这件事上不断叠加技术幻想。结果通常不是系统更强，而是项目更难落地。</p><p>如果把目标拉回到现实，一个博客系统的本质其实非常清晰：它首先是一个内容管理系统，其次是一个阅读体验系统，最后才是一个增长和订阅系统。围绕这个本质去选技术，Spring Boot 加 Vue 之所以合适，不是因为它们最潮，而是因为它们在这个问题上的边界非常清晰。</p><h2>后端为什么选 Spring Boot</h2><p>博客后端看上去简单，实际上很考验结构设计。文章管理、分类标签、系列、评论、订阅、搜索、权限、媒体上传、SEO 配置、数据统计，这些模块单个看都不复杂，但一旦放在一起，就要求后端有很强的模块化稳定性。Spring Boot 的优势就在这里：它不是为了炫技存在，而是为了让你把系统拆清楚。</p><h2>前端为什么选 Vue</h2><p>Vue 的优势不是能做页面，而是它让页面的组织方式很自然。一个博客前台并不是只有文章详情页，它至少还会有首页、分类页、标签页、归档页、系列页、搜索页和 About 页；后台则需要仪表盘、文章管理、编辑器、评论管理、媒体库和站点设置。</p><h2>技术栈选择的关键</h2><p>如果目标是做一套自己的系统、作为作品或者长期维护的产品，那你需要的是控制力，而不是单纯快速搭建。</p><h2>总结</h2><p>个人博客系统不是为了证明你会多少框架，而是为了构建一个能写、能看、能搜、能增长、能长期维护的内容产品。</p>',
JSON_ARRAY(
  JSON_OBJECT('text','后端为什么选 Spring Boot','id','backend-why','level',2),
  JSON_OBJECT('text','前端为什么选 Vue','id','frontend-why','level',2),
  JSON_OBJECT('text','技术栈选择的关键','id','stack-key','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
8
FROM article a WHERE a.slug = 'springboot-vue-blog-tech-stack'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
'# 博客后台管理系统应该包含哪些核心功能\n\n很多人做博客产品时，注意力天然会被前台吸引。首页够不够漂亮，文章详情页排版好不好看，深色模式有没有质感。这些当然重要，但它们只解决了一个问题：读者第一次点进来时，看起来顺不顺眼。真正决定一个博客系统能不能长期活下去的，其实是后台。\n\n## 后台的真正角色\n\n对于作者来说，博客不是一个静态展示页，而是一个持续生产内容的工作台。作者要在这里写文章、改文章、管理分类标签、处理评论、上传图片、配置首页、看数据、发 Newsletter。换句话说，后台不是附属页面，它本质上是一个内容生产系统。\n\n## 低摩擦比堆功能更重要\n\n一个真正好用的博客后台，首先要解决的是低摩擦。作者写作时最怕被打断：保存要不要手动点？预览和编辑切换麻不麻烦？插图是不是要走好几步？这些看似都是小事，但内容生产是高频动作，小阻力累积起来就会变成大厌烦。\n\n## 后台会塑造作者行为\n\n系统会潜移默化地决定作者更愿意做什么。如果添加标签很顺手，作者就更可能认真维护标签体系；如果文章系列创建方便，作者就更可能写连续内容。\n\n## 总结\n\n前台决定读者第一眼是否舒服，后台决定作者能不能一直写下去。',
'<h1>博客后台管理系统应该包含哪些核心功能</h1><p>很多人做博客产品时，注意力天然会被前台吸引。但真正决定一个博客系统能不能长期活下去的，其实是后台。</p><h2>后台的真正角色</h2><p>对于作者来说，博客不是一个静态展示页，而是一个持续生产内容的工作台。后台不是附属页面，它本质上是一个内容生产系统。</p><h2>低摩擦比堆功能更重要</h2><p>一个真正好用的博客后台，首先要解决的是低摩擦。内容生产是高频动作，小阻力累积起来就会变成大厌烦。</p><h2>后台会塑造作者行为</h2><p>系统会潜移默化地决定作者更愿意做什么。如果添加标签很顺手，作者就更可能认真维护标签体系。</p><h2>总结</h2><p>前台决定读者第一眼是否舒服，后台决定作者能不能一直写下去。</p>',
JSON_ARRAY(
  JSON_OBJECT('text','后台的真正角色','id','admin-role','level',2),
  JSON_OBJECT('text','低摩擦比堆功能更重要','id','low-friction','level',2),
  JSON_OBJECT('text','后台会塑造作者行为','id','shape-behavior','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
7
FROM article a WHERE a.slug = 'blog-admin-dashboard-core-features'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
'# 如何设计文章分类、标签和系列文集\n\n分类、标签、系列、归档看上去都像是内容组织方式，但它们解决的是不同层级的问题。\n\n## 分类解决大主题\n\n分类适合处理大方向，例如技术、产品、运营、设计。它让用户在第一次进入博客时，能快速建立对内容范围的理解。\n\n## 标签解决细粒度描述\n\n标签不是分类的重复，而是对文章主题的补充描述。比如 Spring Boot、SEO、Newsletter、推荐系统。好的标签体系有助于搜索、相关推荐和知识聚合。\n\n## 系列解决连续阅读\n\n系列适合处理需要按顺序阅读的内容，例如从 0 到 1 做个人博客系统。它不是单纯分组，而是在构建阅读路径。\n\n## 归档解决时间维度\n\n归档不是主题维度，而是时间维度。它帮助用户从时间轴理解博客的发展。\n\n## 总结\n\n分类、标签、系列、归档需要同时存在，分别服务于主题定位、细粒度描述、连续阅读与时间回看。',
'<h1>如何设计文章分类、标签和系列文集</h1><p>分类、标签、系列、归档看上去都像是内容组织方式，但它们解决的是不同层级的问题。</p><h2>分类解决大主题</h2><p>分类适合处理大方向，例如技术、产品、运营、设计。它让用户在第一次进入博客时，能快速建立对内容范围的理解。</p><h2>标签解决细粒度描述</h2><p>标签不是分类的重复，而是对文章主题的补充描述。比如 Spring Boot、SEO、Newsletter、推荐系统。</p><h2>系列解决连续阅读</h2><p>系列适合处理需要按顺序阅读的内容，例如从 0 到 1 做个人博客系统。它不是单纯分组，而是在构建阅读路径。</p><h2>归档解决时间维度</h2><p>归档不是主题维度，而是时间维度。它帮助用户从时间轴理解博客的发展。</p><h2>总结</h2><p>分类、标签、系列、归档需要同时存在。</p>',
JSON_ARRAY(
  JSON_OBJECT('text','分类解决大主题','id','cat-topic','level',2),
  JSON_OBJECT('text','标签解决细粒度描述','id','tag-granular','level',2),
  JSON_OBJECT('text','系列解决连续阅读','id','series-read','level',2),
  JSON_OBJECT('text','归档解决时间维度','id','archive-time','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
6
FROM article a WHERE a.slug = 'design-blog-category-tag-series'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
'# 个人博客系统数据库表设计详解\n\n数据库设计要兼顾当前需求与未来扩展。博客系统虽然看起来是一个轻量项目，但一旦包含分类、标签、系列、评论、订阅、推荐和统计，就会迅速变成一个结构化内容系统。\n\n## 核心表拆分思路\n\n文章主表负责元信息，文章正文表负责正文内容。这种拆分可以让列表查询更轻，也方便后续做版本历史、全文搜索与缓存。\n\n## 为什么正文单独存储\n\n列表页、首页、搜索页通常只关心标题、摘要、封面、状态、发布时间和计数，而文章详情页才需要正文。把正文放在独立表里，可以显著降低高频列表查询的负担。\n\n## 分类、标签与系列的关系\n\n分类负责大主题，标签负责细粒度描述，系列负责顺序阅读。三者不是重复建模，而是不同阅读入口。\n\n## 评论与订阅为什么要独立\n\n评论属于互动域，订阅属于增长域。它们和文章有强关联，但在数据生命周期和查询方式上与正文不同，应该独立设计。\n\n## 总结\n\n好的表设计会直接影响接口复杂度、前后端联调效率和后续系统扩展成本。',
'<h1>个人博客系统数据库表设计详解</h1><p>数据库设计要兼顾当前需求与未来扩展。博客系统虽然看起来是一个轻量项目，但一旦包含分类、标签、系列、评论、订阅、推荐和统计，就会迅速变成一个结构化内容系统。</p><h2>核心表拆分思路</h2><p>文章主表负责元信息，文章正文表负责正文内容。这种拆分可以让列表查询更轻，也方便后续做版本历史、全文搜索与缓存。</p><h2>为什么正文单独存储</h2><p>列表页、首页、搜索页通常只关心标题、摘要、封面、状态、发布时间和计数，而文章详情页才需要正文。</p><h2>分类、标签与系列的关系</h2><p>分类负责大主题，标签负责细粒度描述，系列负责顺序阅读。</p><h2>评论与订阅为什么要独立</h2><p>评论属于互动域，订阅属于增长域。它们和文章有强关联，但在数据生命周期和查询方式上与正文不同。</p><h2>总结</h2><p>好的表设计会直接影响接口复杂度、前后端联调效率和后续系统扩展成本。</p>',
JSON_ARRAY(
  JSON_OBJECT('text','核心表拆分思路','id','core-split','level',2),
  JSON_OBJECT('text','为什么正文单独存储','id','content-separate','level',2),
  JSON_OBJECT('text','分类、标签与系列的关系','id','taxonomy-rel','level',2),
  JSON_OBJECT('text','评论与订阅为什么要独立','id','comment-subscriber','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
10
FROM article a WHERE a.slug = 'blog-database-schema-design'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
'# 博客推荐系统怎么做得更智能\n\n只按标签推荐很容易出现低相关、重复和冷启动问题。博客推荐真正要解决的是阅读深度，而不是简单找几篇“可能差不多”的文章。\n\n## 召回层\n\n候选文章不应只来自标签。还可以来自分类、内容相似度、系列关系、热门文章和用户行为。\n\n## 排序层\n\n对候选文章做打分时，至少要综合内容相似、标签重合、系列关系、热度、新鲜度和人工加权。\n\n## 重排层\n\n重排负责去重、多样性控制、新文扶持与曝光均衡。没有重排，推荐列表往往会看上去很笨。\n\n## 推荐位也要分场景\n\n文章详情页、首页、404 页和搜索无结果页的推荐目标并不一样。详情页追求强相关，首页追求点击率和多样性。\n\n## 总结\n\n推荐系统不是一个接口，而是一套围绕内容分发、阅读路径和增长目标的完整机制。',
'<h1>博客推荐系统怎么做得更智能</h1><p>只按标签推荐很容易出现低相关、重复和冷启动问题。博客推荐真正要解决的是阅读深度，而不是简单找几篇可能差不多的文章。</p><h2>召回层</h2><p>候选文章不应只来自标签。还可以来自分类、内容相似度、系列关系、热门文章和用户行为。</p><h2>排序层</h2><p>对候选文章做打分时，至少要综合内容相似、标签重合、系列关系、热度、新鲜度和人工加权。</p><h2>重排层</h2><p>重排负责去重、多样性控制、新文扶持与曝光均衡。没有重排，推荐列表往往会看上去很笨。</p><h2>推荐位也要分场景</h2><p>文章详情页、首页、404 页和搜索无结果页的推荐目标并不一样。</p><h2>总结</h2><p>推荐系统不是一个接口，而是一套围绕内容分发、阅读路径和增长目标的完整机制。</p>',
JSON_ARRAY(
  JSON_OBJECT('text','召回层','id','recall','level',2),
  JSON_OBJECT('text','排序层','id','ranking','level',2),
  JSON_OBJECT('text','重排层','id','rerank','level',2),
  JSON_OBJECT('text','推荐位也要分场景','id','scene','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
11
FROM article a WHERE a.slug = 'advanced-blog-recommendation-system'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

-- shorter but still full enough for remaining articles
INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
CONCAT('# 使用 Vue 3 实现博客前台首页\n\n首页既要展示品牌，也要承担内容分发和订阅转化。\n\n## 组件拆分\n\n建议拆成顶部导航、Hero 区、最新文章、热门文章、系列模块和订阅模块。\n\n## 数据驱动渲染\n\n首页不应该写死内容，而应该由接口返回精选、最新和热门文章列表，由组件负责渲染。\n\n## 响应式优先\n\n首页必须优先考虑移动端体验。\n\n## 总结\n\n用 Vue 3 做首页时，建议数据驱动渲染和组件化拆分。'),
CONCAT('<h1>使用 Vue 3 实现博客前台首页</h1><p>首页既要展示品牌，也要承担内容分发和订阅转化。</p><h2>组件拆分</h2><p>建议拆成顶部导航、Hero 区、最新文章、热门文章、系列模块和订阅模块。</p><h2>数据驱动渲染</h2><p>首页不应该写死内容，而应该由接口返回精选、最新和热门文章列表，由组件负责渲染。</p><h2>响应式优先</h2><p>首页必须优先考虑移动端体验。</p><h2>总结</h2><p>用 Vue 3 做首页时，建议数据驱动渲染和组件化拆分。</p>'),
JSON_ARRAY(
  JSON_OBJECT('text','组件拆分','id','split','level',2),
  JSON_OBJECT('text','数据驱动渲染','id','data-driven','level',2),
  JSON_OBJECT('text','响应式优先','id','responsive','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
7
FROM article a WHERE a.slug = 'build-blog-homepage-with-vue3'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
CONCAT('# Spring Security + JWT 登录认证实战\n\n后台管理必须保护文章、评论和站点配置。\n\n## 技术方案\n\n使用 Spring Security 负责认证拦截，JWT 负责接口访问凭证。\n\n## 核心流程\n\n用户登录后，服务端验证账号密码并签发 token，前端在后续请求中携带 token。\n\n## 权限控制\n\n角色和权限要分离，文章发布、评论审核和系统设置应有不同权限。\n\n## 总结\n\n登录认证是后台系统的第一道门槛，必须优先做好。'),
CONCAT('<h1>Spring Security + JWT 登录认证实战</h1><p>后台管理必须保护文章、评论和站点配置。</p><h2>技术方案</h2><p>使用 Spring Security 负责认证拦截，JWT 负责接口访问凭证。</p><h2>核心流程</h2><p>用户登录后，服务端验证账号密码并签发 token，前端在后续请求中携带 token。</p><h2>权限控制</h2><p>角色和权限要分离，文章发布、评论审核和系统设置应有不同权限。</p><h2>总结</h2><p>登录认证是后台系统的第一道门槛，必须优先做好。</p>'),
JSON_ARRAY(
  JSON_OBJECT('text','技术方案','id','solution','level',2),
  JSON_OBJECT('text','核心流程','id','flow','level',2),
  JSON_OBJECT('text','权限控制','id','permission','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
9
FROM article a WHERE a.slug = 'spring-security-jwt-auth-practice'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
CONCAT('# Markdown 编辑器模块的设计与实现\n\n编辑器需要兼顾写作体验、预览能力和存储结构。\n\n## 核心能力\n\nMarkdown 输入、实时预览、图片上传、代码高亮和自动生成目录。\n\n## 存储方案\n\n建议同时保存 Markdown 原文和 HTML 渲染结果。\n\n## 为什么要保留 TOC\n\n文章详情页可以直接渲染目录，提高阅读效率。\n\n## 总结\n\n编辑器是作者最常使用的模块，体验必须稳定顺滑。'),
CONCAT('<h1>Markdown 编辑器模块的设计与实现</h1><p>编辑器需要兼顾写作体验、预览能力和存储结构。</p><h2>核心能力</h2><p>Markdown 输入、实时预览、图片上传、代码高亮和自动生成目录。</p><h2>存储方案</h2><p>建议同时保存 Markdown 原文和 HTML 渲染结果。</p><h2>为什么要保留 TOC</h2><p>文章详情页可以直接渲染目录，提高阅读效率。</p><h2>总结</h2><p>编辑器是作者最常使用的模块，体验必须稳定顺滑。</p>'),
JSON_ARRAY(
  JSON_OBJECT('text','核心能力','id','core','level',2),
  JSON_OBJECT('text','存储方案','id','storage','level',2),
  JSON_OBJECT('text','为什么要保留 TOC','id','toc','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
8
FROM article a WHERE a.slug = 'markdown-editor-design-and-implementation'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
CONCAT('# 博客 SEO 优化完整方案\n\n博客 SEO 的目标不是制造爆发流量，而是让值得被看到的文章长期被找到。\n\n## 内容表达优先\n\n标题要准确，摘要要明确，正文结构要清晰。\n\n## 技术配置\n\n每篇文章应有独立 title 和 description，站点应提供 sitemap.xml、robots.txt 和结构化数据。\n\n## 页面体验\n\n加载速度、移动端适配和语义化结构都会影响搜索表现。\n\n## 总结\n\nSEO 是内容质量、页面结构和技术细节共同作用的结果。'),
CONCAT('<h1>博客 SEO 优化完整方案</h1><p>博客 SEO 的目标不是制造爆发流量，而是让值得被看到的文章长期被找到。</p><h2>内容表达优先</h2><p>标题要准确，摘要要明确，正文结构要清晰。</p><h2>技术配置</h2><p>每篇文章应有独立 title 和 description，站点应提供 sitemap.xml、robots.txt 和结构化数据。</p><h2>页面体验</h2><p>加载速度、移动端适配和语义化结构都会影响搜索表现。</p><h2>总结</h2><p>SEO 是内容质量、页面结构和技术细节共同作用的结果。</p>'),
JSON_ARRAY(
  JSON_OBJECT('text','内容表达优先','id','content-first','level',2),
  JSON_OBJECT('text','技术配置','id','tech-config','level',2),
  JSON_OBJECT('text','页面体验','id','ux','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
9
FROM article a WHERE a.slug = 'complete-blog-seo-optimization-guide'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
CONCAT('# Newsletter 订阅系统设计\n\n博客不应只被访问一次，更应形成长期订阅关系。\n\n## 核心环节\n\n订阅表单、双重确认、欢迎邮件、分组发送和打开率点击率统计。\n\n## 为什么它重要\n\n订阅系统把一次访问转成可持续触达关系。\n\n## 与博客发布的关系\n\n高质量文章发布后可以同步发送 Newsletter，形成 Web 与 Email 的双渠道。\n\n## 总结\n\n订阅系统是博客增长能力的重要组成部分。'),
CONCAT('<h1>Newsletter 订阅系统设计</h1><p>博客不应只被访问一次，更应形成长期订阅关系。</p><h2>核心环节</h2><p>订阅表单、双重确认、欢迎邮件、分组发送和打开率点击率统计。</p><h2>为什么它重要</h2><p>订阅系统把一次访问转成可持续触达关系。</p><h2>与博客发布的关系</h2><p>高质量文章发布后可以同步发送 Newsletter，形成 Web 与 Email 的双渠道。</p><h2>总结</h2><p>订阅系统是博客增长能力的重要组成部分。</p>'),
JSON_ARRAY(
  JSON_OBJECT('text','核心环节','id','core','level',2),
  JSON_OBJECT('text','为什么它重要','id','importance','level',2),
  JSON_OBJECT('text','与博客发布的关系','id','relationship','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
8
FROM article a WHERE a.slug = 'newsletter-subscription-system-design'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
CONCAT('# 暗黑模式与阅读体验优化\n\n暗黑模式不只是换背景色，而是阅读体验、对比度、可访问性和主题系统的综合设计。\n\n## 为什么容易翻车\n\n因为暗色界面层次天然更难建立，如果只是简单反色，信息会黏在一起。\n\n## 博客场景的重点\n\n博客页面的核心任务是长时间阅读，因此设计重点不在醒目，而在稳定。\n\n## 可访问性\n\n色彩对比、焦点态、链接识别和代码块表现都必须认真设计。\n\n## 总结\n\n好的暗黑模式应该让用户更专注内容，而不是更注意界面。'),
CONCAT('<h1>暗黑模式与阅读体验优化</h1><p>暗黑模式不只是换背景色，而是阅读体验、对比度、可访问性和主题系统的综合设计。</p><h2>为什么容易翻车</h2><p>因为暗色界面层次天然更难建立，如果只是简单反色，信息会黏在一起。</p><h2>博客场景的重点</h2><p>博客页面的核心任务是长时间阅读，因此设计重点不在醒目，而在稳定。</p><h2>可访问性</h2><p>色彩对比、焦点态、链接识别和代码块表现都必须认真设计。</p><h2>总结</h2><p>好的暗黑模式应该让用户更专注内容，而不是更注意界面。</p>'),
JSON_ARRAY(
  JSON_OBJECT('text','为什么容易翻车','id','fail','level',2),
  JSON_OBJECT('text','博客场景的重点','id','blog-focus','level',2),
  JSON_OBJECT('text','可访问性','id','a11y','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
6
FROM article a WHERE a.slug = 'dark-mode-and-reading-experience'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
CONCAT('# 从 0 开发个人博客系统项目复盘\n\n复盘的意义不只是回顾结果，而是沉淀方法。\n\n## 项目过程\n\n需求整理、模块拆分、数据库设计、后端接口开发、前端页面实现和上线优化，构成了整个项目的主线。\n\n## 最大的收获\n\n真正重要的不是一开始就做很多，而是先把内容生产和阅读闭环跑通。\n\n## 遇到的问题\n\n表结构假设和实际实现不一致、初始化数据不完整、前后台联调时字段命名不统一，都是很典型的问题。\n\n## 总结\n\n一个好的复盘，不只是总结结果，更是总结方法和边界。'),
CONCAT('<h1>从 0 开发个人博客系统项目复盘</h1><p>复盘的意义不只是回顾结果，而是沉淀方法。</p><h2>项目过程</h2><p>需求整理、模块拆分、数据库设计、后端接口开发、前端页面实现和上线优化，构成了整个项目的主线。</p><h2>最大的收获</h2><p>真正重要的不是一开始就做很多，而是先把内容生产和阅读闭环跑通。</p><h2>遇到的问题</h2><p>表结构假设和实际实现不一致、初始化数据不完整、前后台联调时字段命名不统一，都是很典型的问题。</p><h2>总结</h2><p>一个好的复盘，不只是总结结果，更是总结方法和边界。</p>'),
JSON_ARRAY(
  JSON_OBJECT('text','项目过程','id','process','level',2),
  JSON_OBJECT('text','最大的收获','id','gain','level',2),
  JSON_OBJECT('text','遇到的问题','id','issues','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
10
FROM article a WHERE a.slug = 'blog-system-project-retrospective'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
CONCAT('# 长期写作最难的，从来不是没有时间，而是没有稳定节奏\n\n很多人觉得写不下去是因为太忙，但长期看，真正让写作中断的往往不是时间不够，而是节奏不稳定、心理门槛太高。\n\n## 节奏为什么更重要\n\n时间不足通常只是表象。真正让人停下来的是一旦中断几天，重新开始的心理门槛会迅速变高。\n\n## 如何降低启动门槛\n\n不把写作理解成一次必须完整交付的作品，而是理解成一种长期维持的节奏。提纲、观察和复盘也算写。\n\n## 总结\n\n一个人能不能持续写，决定因素不是有没有很多空闲时间，而是有没有找到一种能长期维持的节奏。'),
CONCAT('<h1>长期写作最难的，从来不是没有时间，而是没有稳定节奏</h1><p>很多人觉得写不下去是因为太忙，但长期看，真正让写作中断的往往不是时间不够，而是节奏不稳定、心理门槛太高。</p><h2>节奏为什么更重要</h2><p>真正让人停下来的是一旦中断几天，重新开始的心理门槛会迅速变高。</p><h2>如何降低启动门槛</h2><p>不把写作理解成一次必须完整交付的作品，而是理解成一种长期维持的节奏。</p><h2>总结</h2><p>一个人能不能持续写，决定因素不是有没有很多空闲时间，而是有没有找到一种能长期维持的节奏。</p>'),
JSON_ARRAY(
  JSON_OBJECT('text','节奏为什么更重要','id','rhythm','level',2),
  JSON_OBJECT('text','如何降低启动门槛','id','low-barrier','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
7
FROM article a WHERE a.slug = 'writing-consistency-matters-more-than-time'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

INSERT INTO article_content (article_id, content_md, content_html, toc_json, reading_time)
SELECT a.id,
CONCAT('# 我越来越不想做“看起来很厉害”的东西了\n\n随着做的项目越来越多，我越来越发现，真正值得投入的，不是那些第一眼看起来很厉害的东西，而是那些能长期使用、长期积累、长期留下来的东西。\n\n## 表面厉害和真实价值不是一回事\n\n一个首页可以做得非常抓眼，但后台写作流程可能一团糟；一个推荐系统可以讲得很高级，但实际文章数量少得撑不起推荐逻辑。\n\n## 我越来越在意什么\n\n第一眼不一定惊艳，但越用越顺、越做越稳、越久越有积累的东西。\n\n## 总结\n\n真正值得被信任的，不是瞬间制造出的强烈印象，而是那些经过使用和时间验证后仍然站得住的部分。'),
CONCAT('<h1>我越来越不想做看起来很厉害的东西了</h1><p>随着做的项目越来越多，我越来越发现，真正值得投入的，不是那些第一眼看起来很厉害的东西，而是那些能长期使用、长期积累、长期留下来的东西。</p><h2>表面厉害和真实价值不是一回事</h2><p>一个首页可以做得非常抓眼，但后台写作流程可能一团糟；一个推荐系统可以讲得很高级，但实际文章数量少得撑不起推荐逻辑。</p><h2>我越来越在意什么</h2><p>第一眼不一定惊艳，但越用越顺、越做越稳、越久越有积累的东西。</p><h2>总结</h2><p>真正值得被信任的，不是瞬间制造出的强烈印象，而是那些经过使用和时间验证后仍然站得住的部分。</p>'),
JSON_ARRAY(
  JSON_OBJECT('text','表面厉害和真实价值不是一回事','id','surface-vs-value','level',2),
  JSON_OBJECT('text','我越来越在意什么','id','what-matters','level',2),
  JSON_OBJECT('text','总结','id','summary','level',2)
),
6
FROM article a WHERE a.slug = 'i-no-longer-chase-impressive-things'
ON DUPLICATE KEY UPDATE content_md = VALUES(content_md), content_html = VALUES(content_html), toc_json = VALUES(toc_json), reading_time = VALUES(reading_time);

-- =========================================================
-- 7) Tag relationships
-- =========================================================
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'spring-boot' WHERE a.slug = 'springboot-vue-blog-tech-stack';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'vue-3' WHERE a.slug = 'springboot-vue-blog-tech-stack';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'content-platform' WHERE a.slug = 'springboot-vue-blog-tech-stack';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'mvp' WHERE a.slug = 'springboot-vue-blog-tech-stack';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'admin-dashboard' WHERE a.slug = 'blog-admin-dashboard-core-features';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'prd' WHERE a.slug = 'blog-admin-dashboard-core-features';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'feature-design' WHERE a.slug = 'blog-admin-dashboard-core-features';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'feature-design' WHERE a.slug = 'design-blog-category-tag-series';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'series-articles' WHERE a.slug = 'design-blog-category-tag-series';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'content-platform' WHERE a.slug = 'design-blog-category-tag-series';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'postgresql' WHERE a.slug = 'blog-database-schema-design';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'spring-boot' WHERE a.slug = 'blog-database-schema-design';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'content-platform' WHERE a.slug = 'blog-database-schema-design';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'recommendation-system' WHERE a.slug = 'advanced-blog-recommendation-system';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'analytics' WHERE a.slug = 'advanced-blog-recommendation-system';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'content-platform' WHERE a.slug = 'advanced-blog-recommendation-system';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'vue-3' WHERE a.slug = 'build-blog-homepage-with-vue3';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'responsive-design' WHERE a.slug = 'build-blog-homepage-with-vue3';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'ui-design' WHERE a.slug = 'build-blog-homepage-with-vue3';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'spring-security' WHERE a.slug = 'spring-security-jwt-auth-practice';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'jwt' WHERE a.slug = 'spring-security-jwt-auth-practice';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'spring-boot' WHERE a.slug = 'spring-security-jwt-auth-practice';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'markdown' WHERE a.slug = 'markdown-editor-design-and-implementation';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'vue-3' WHERE a.slug = 'markdown-editor-design-and-implementation';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'feature-design' WHERE a.slug = 'markdown-editor-design-and-implementation';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'seo' WHERE a.slug = 'complete-blog-seo-optimization-guide';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'json-ld' WHERE a.slug = 'complete-blog-seo-optimization-guide';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'user-growth' WHERE a.slug = 'complete-blog-seo-optimization-guide';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'newsletter' WHERE a.slug = 'newsletter-subscription-system-design';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'user-growth' WHERE a.slug = 'newsletter-subscription-system-design';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'analytics' WHERE a.slug = 'newsletter-subscription-system-design';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'dark-mode' WHERE a.slug = 'dark-mode-and-reading-experience';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'reading-experience' WHERE a.slug = 'dark-mode-and-reading-experience';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'accessibility' WHERE a.slug = 'dark-mode-and-reading-experience';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'project-review' WHERE a.slug = 'blog-system-project-retrospective';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'dev-log' WHERE a.slug = 'blog-system-project-retrospective';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'content-platform' WHERE a.slug = 'blog-system-project-retrospective';

INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'dev-log' WHERE a.slug = 'writing-consistency-matters-more-than-time';
INSERT IGNORE INTO article_tag_rel (article_id, tag_id)
SELECT a.id, t.id FROM article a JOIN article_tag t ON t.slug = 'project-review' WHERE a.slug = 'i-no-longer-chase-impressive-things';

-- =========================================================
-- 8) Series relationships
-- =========================================================
INSERT IGNORE INTO article_series_rel (article_id, series_id, sort_order)
SELECT a.id, s.id, 1 FROM article a JOIN article_series s ON s.slug = 'build-a-blog-system' WHERE a.slug = 'springboot-vue-blog-tech-stack';
INSERT IGNORE INTO article_series_rel (article_id, series_id, sort_order)
SELECT a.id, s.id, 2 FROM article a JOIN article_series s ON s.slug = 'build-a-blog-system' WHERE a.slug = 'blog-admin-dashboard-core-features';
INSERT IGNORE INTO article_series_rel (article_id, series_id, sort_order)
SELECT a.id, s.id, 3 FROM article a JOIN article_series s ON s.slug = 'build-a-blog-system' WHERE a.slug = 'design-blog-category-tag-series';
INSERT IGNORE INTO article_series_rel (article_id, series_id, sort_order)
SELECT a.id, s.id, 4 FROM article a JOIN article_series s ON s.slug = 'build-a-blog-system' WHERE a.slug = 'blog-database-schema-design';
INSERT IGNORE INTO article_series_rel (article_id, series_id, sort_order)
SELECT a.id, s.id, 5 FROM article a JOIN article_series s ON s.slug = 'build-a-blog-system' WHERE a.slug = 'advanced-blog-recommendation-system';

INSERT IGNORE INTO article_series_rel (article_id, series_id, sort_order)
SELECT a.id, s.id, 1 FROM article a JOIN article_series s ON s.slug = 'springboot-vue-fullstack' WHERE a.slug = 'build-blog-homepage-with-vue3';
INSERT IGNORE INTO article_series_rel (article_id, series_id, sort_order)
SELECT a.id, s.id, 2 FROM article a JOIN article_series s ON s.slug = 'springboot-vue-fullstack' WHERE a.slug = 'spring-security-jwt-auth-practice';
INSERT IGNORE INTO article_series_rel (article_id, series_id, sort_order)
SELECT a.id, s.id, 3 FROM article a JOIN article_series s ON s.slug = 'springboot-vue-fullstack' WHERE a.slug = 'markdown-editor-design-and-implementation';

-- =========================================================
-- 9) Basic page_content seed
-- =========================================================
INSERT INTO page_content (id, page_code, title, content_md, content_html, seo_title, seo_description, status, updated_at)
VALUES
(1, 'ABOUT', '关于我', '# 关于我\n\n这里是一位长期写作、持续做产品与工程实践的人。这个博客关注技术、内容系统、阅读体验和长期主义。', '<h1>关于我</h1><p>这里是一位长期写作、持续做产品与工程实践的人。这个博客关注技术、内容系统、阅读体验和长期主义。</p>', '关于我', '博客作者介绍页', 1, NOW()),
(2, 'CONTACT', '联系我', '# 联系我\n\n你可以通过邮箱与我联系：seed_author@blog.com。', '<h1>联系我</h1><p>你可以通过邮箱与我联系：seed_author@blog.com。</p>', '联系我', '博客联系页', 1, NOW()),
(3, 'PRIVACY', '隐私政策', '# 隐私政策\n\n本站尊重并保护用户隐私，仅在必要范围内处理访问统计和订阅信息。', '<h1>隐私政策</h1><p>本站尊重并保护用户隐私，仅在必要范围内处理访问统计和订阅信息。</p>', '隐私政策', '博客隐私政策页面', 1, NOW()),
(4, 'TERMS', '服务条款', '# 服务条款\n\n访问本站即表示你同意以合法、合理的方式使用本站内容与服务。', '<h1>服务条款</h1><p>访问本站即表示你同意以合法、合理的方式使用本站内容与服务。</p>', '服务条款', '博客服务条款页面', 1, NOW())
ON DUPLICATE KEY UPDATE
  title = VALUES(title), content_md = VALUES(content_md), content_html = VALUES(content_html),
  seo_title = VALUES(seo_title), seo_description = VALUES(seo_description), status = VALUES(status), updated_at = NOW();

-- =========================================================
-- 10) Site config refresh / add
-- =========================================================
INSERT INTO site_config (config_key, config_value, description)
VALUES
  ('site_title', 'Bob 的个人博客', 'Site title'),
  ('site_description', '一个关于技术、产品、设计与长期主义的个人博客', 'Site description'),
  ('site_keywords', 'blog,technology,product,design,newsletter', 'SEO keywords'),
  ('site_author', 'Bob', 'Author name'),
  ('site_footer', '© 2026 Bob Blog. All rights reserved.', 'Footer text'),
  ('social_email', 'seed_author@blog.com', 'Contact email')
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value),
  description = VALUES(description);

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
