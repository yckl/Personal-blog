-- Seed Digital Products
INSERT IGNORE INTO digital_product (title, slug, description, cover_image, product_type, price_cents, currency, file_url, file_size_bytes, preview_url, status, visibility, download_count, sort_order) VALUES
('Vue 3 Advanced Techniques', 'vue-3-advanced', 'Master Vue 3 Composition API, state management, and performance.', 'https://picsum.photos/seed/dp1/800/600', 'EBOOK', 1900, 'USD', '/files/dummy.pdf', 1024000, '', 'PUBLISHED', 'PUBLIC', 125, 10),
('React Design Patterns', 'react-design-patterns', 'Learn how top engineers architect React applications.', 'https://picsum.photos/seed/dp2/800/600', 'EBOOK', 2900, 'USD', '/files/dummy.pdf', 2048000, '', 'PUBLISHED', 'PUBLIC', 89, 20),
('Fullstack Journey', 'fullstack-journey', 'A complete guide to becoming a modern fullstack developer.', 'https://picsum.photos/seed/dp3/800/600', 'EBOOK', 0, 'USD', '/files/dummy.pdf', 500000, '', 'PUBLISHED', 'PUBLIC', 430, 30),
('CSS for Backend Devs', 'css-for-backend', 'Quickly master CSS grids, flexbox, and modern features.', 'https://picsum.photos/seed/dp4/800/600', 'EBOOK', 900, 'USD', '/files/dummy.pdf', 1500000, '', 'PUBLISHED', 'PUBLIC', 200, 40),
('Interview Guide', 'interview-guide', 'Top 100 questions and solutions for frontend interviews.', 'https://picsum.photos/seed/dp5/800/600', 'EBOOK', 1500, 'USD', '/files/dummy.pdf', 800000, '', 'PUBLISHED', 'MEMBER_ONLY', 35, 50),

('Mastering Spring Boot', 'mastering-spring-boot', 'Comprehensive video course on building REST APIs.', 'https://picsum.photos/seed/dp6/800/600', 'COURSE', 9900, 'USD', '/files/dummy.zip', 500000000, '', 'PUBLISHED', 'PUBLIC', 12, 60),
('Vue 3 Masterclass', 'vue3-masterclass', 'Over 20 hours of HD video content building 5 real apps.', 'https://picsum.photos/seed/dp7/800/600', 'COURSE', 14900, 'USD', '/files/dummy.zip', 800000000, '', 'PUBLISHED', 'PREMIUM_ONLY', 5, 70),
('Database Tuning', 'db-tuning-course', 'Improve your SQL performance by 10x.', 'https://picsum.photos/seed/dp8/800/600', 'COURSE', 4900, 'USD', '/files/dummy.zip', 200000000, '', 'PUBLISHED', 'PUBLIC', 48, 80),
('Figma for Developers', 'figma-devs-course', 'Translate designs to code perfectly every time.', 'https://picsum.photos/seed/dp9/800/600', 'COURSE', 5900, 'USD', '/files/dummy.zip', 300000000, '', 'PUBLISHED', 'PUBLIC', 76, 90),
('DevOps 101', 'devops-101', 'Docker, CI/CD, and Kubernetes basics for devs.', 'https://picsum.photos/seed/dp10/800/600', 'COURSE', 8900, 'USD', '/files/dummy.zip', 400000000, '', 'PUBLISHED', 'PUBLIC', 92, 100),

('Premium UI Kit', 'premium-ui-kit', '100+ high-quality Tailwind components ready to copy-paste.', 'https://picsum.photos/seed/dp11/800/600', 'RESOURCE_PACK', 4900, 'USD', '/files/dummy.zip', 5000000, '', 'PUBLISHED', 'PUBLIC', 412, 110),
('Icon Set Minimal', 'icon-set-minimal', '500 handcrafted SVG icons for modern interfaces.', 'https://picsum.photos/seed/dp12/800/600', 'RESOURCE_PACK', 2900, 'USD', '/files/dummy.zip', 2000000, '', 'PUBLISHED', 'PUBLIC', 800, 120),
('Notion Templates', 'notion-templates-pack', 'My personal Notion templates for productivity.', 'https://picsum.photos/seed/dp13/800/600', 'RESOURCE_PACK', 1900, 'USD', '/files/dummy.zip', 1000000, '', 'PUBLISHED', 'PUBLIC', 1050, 130),
('Resume Templates', 'resume-templates', 'ATS-friendly developer resume templates (Word/Figma).', 'https://picsum.photos/seed/dp14/800/600', 'RESOURCE_PACK', 0, 'USD', '/files/dummy.zip', 500000, '', 'PUBLISHED', 'PUBLIC', 2300, 140),
('Background Textures', 'bg-textures-pack', 'High-res abstract backgrounds for web projects.', 'https://picsum.photos/seed/dp15/800/600', 'RESOURCE_PACK', 1000, 'USD', '/files/dummy.zip', 45000000, '', 'PUBLISHED', 'PUBLIC', 123, 150);

-- Seed Media Files
INSERT IGNORE INTO media_file (original_name, stored_name, file_path, file_url, file_type, mime_type, file_size, width, height, uploaded_by) VALUES
('mountain.jpg', 'img01.jpg', '/uploads/img01.jpg', 'https://picsum.photos/seed/m1/800/600', 'image', 'image/jpeg', 102400, 800, 600, 1),
('river.jpg', 'img02.jpg', '/uploads/img02.jpg', 'https://picsum.photos/seed/m2/800/600', 'image', 'image/jpeg', 102400, 800, 600, 1),
('city.jpg', 'img03.jpg', '/uploads/img03.jpg', 'https://picsum.photos/seed/m3/800/600', 'image', 'image/jpeg', 102400, 800, 600, 1),
('forest.jpg', 'img04.jpg', '/uploads/img04.jpg', 'https://picsum.photos/seed/m4/800/600', 'image', 'image/jpeg', 102400, 800, 600, 1),
('desert.jpg', 'img05.jpg', '/uploads/img05.jpg', 'https://picsum.photos/seed/m5/800/600', 'image', 'image/jpeg', 102400, 800, 600, 1),

('intro.mp4', 'vid01.mp4', '/uploads/vid01.mp4', 'https://www.w3schools.com/html/mov_bbb.mp4', 'video', 'video/mp4', 10240000, 1920, 1080, 1),
('tutorial.mp4', 'vid02.mp4', '/uploads/vid02.mp4', 'https://www.w3schools.com/html/mov_bbb.mp4', 'video', 'video/mp4', 10240000, 1920, 1080, 1),
('demo.mp4', 'vid03.mp4', '/uploads/vid03.mp4', 'https://www.w3schools.com/html/mov_bbb.mp4', 'video', 'video/mp4', 10240000, 1920, 1080, 1),
('interview.mp4', 'vid04.mp4', '/uploads/vid04.mp4', 'https://www.w3schools.com/html/mov_bbb.mp4', 'video', 'video/mp4', 10240000, 1920, 1080, 1),
('vlog.mp4', 'vid05.mp4', '/uploads/vid05.mp4', 'https://www.w3schools.com/html/mov_bbb.mp4', 'video', 'video/mp4', 10240000, 1920, 1080, 1),

('podcast1.mp3', 'aud01.mp3', '/uploads/aud01.mp3', 'https://www.w3schools.com/html/horse.mp3', 'audio', 'audio/mpeg', 5120000, null, null, 1),
('podcast2.mp3', 'aud02.mp3', '/uploads/aud02.mp3', 'https://www.w3schools.com/html/horse.mp3', 'audio', 'audio/mpeg', 5120000, null, null, 1),
('bgm.mp3', 'aud03.mp3', '/uploads/aud03.mp3', 'https://www.w3schools.com/html/horse.mp3', 'audio', 'audio/mpeg', 5120000, null, null, 1),
('voiceover.mp3', 'aud04.mp3', '/uploads/aud04.mp3', 'https://www.w3schools.com/html/horse.mp3', 'audio', 'audio/mpeg', 5120000, null, null, 1),
('sfx.mp3', 'aud05.mp3', '/uploads/aud05.mp3', 'https://www.w3schools.com/html/horse.mp3', 'audio', 'audio/mpeg', 5120000, null, null, 1),

('report.pdf', 'doc01.pdf', '/uploads/doc01.pdf', '/files/doc01.pdf', 'document', 'application/pdf', 204800, null, null, 1),
('invoice.pdf', 'doc02.pdf', '/uploads/doc02.pdf', '/files/doc02.pdf', 'document', 'application/pdf', 204800, null, null, 1),
('resume.pdf', 'doc03.pdf', '/uploads/doc03.pdf', '/files/doc03.pdf', 'document', 'application/pdf', 204800, null, null, 1),
('guide.pdf', 'doc04.pdf', '/uploads/doc04.pdf', '/files/doc04.pdf', 'document', 'application/pdf', 204800, null, null, 1),
('cheatsheet.pdf', 'doc05.pdf', '/uploads/doc05.pdf', '/files/doc05.pdf', 'document', 'application/pdf', 204800, null, null, 1);

-- Seed Menus
INSERT IGNORE INTO menu_config (name, url, icon, sort_order, parent_id, visible, is_external) VALUES
('首页', '/', 'HomeFilled', 10, 0, TRUE, FALSE),
('文章', '/articles', 'List', 20, 0, TRUE, FALSE),
('归档', '/archive', 'Calendar', 30, 0, TRUE, FALSE),
('关于', '/about', 'User', 40, 0, TRUE, FALSE),
('联系', '/contact', 'Message', 50, 0, TRUE, FALSE);
