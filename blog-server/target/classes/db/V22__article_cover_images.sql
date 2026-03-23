-- V22: Add cover images to all seed articles
-- Images are stored in uploads/covers/ and served via /files/covers/

UPDATE article SET cover_image = '/files/covers/tech-stack.png'
WHERE slug = 'springboot-vue-blog-tech-stack';

UPDATE article SET cover_image = '/files/covers/admin-features.png'
WHERE slug = 'blog-admin-dashboard-core-features';

UPDATE article SET cover_image = '/files/covers/taxonomy-design.png'
WHERE slug = 'design-blog-category-tag-series';

UPDATE article SET cover_image = '/files/covers/database-design.png'
WHERE slug = 'blog-database-schema-design';

UPDATE article SET cover_image = '/files/covers/recommendation.png'
WHERE slug = 'advanced-blog-recommendation-system';

UPDATE article SET cover_image = '/files/covers/vue3-homepage.png'
WHERE slug = 'build-blog-homepage-with-vue3';

UPDATE article SET cover_image = '/files/covers/jwt-auth.png'
WHERE slug = 'spring-security-jwt-auth-practice';

UPDATE article SET cover_image = '/files/covers/markdown-editor.png'
WHERE slug = 'markdown-editor-design-and-implementation';

UPDATE article SET cover_image = '/files/covers/seo-guide.png'
WHERE slug = 'complete-blog-seo-optimization-guide';

UPDATE article SET cover_image = '/files/covers/newsletter.png'
WHERE slug = 'newsletter-subscription-system-design';

UPDATE article SET cover_image = '/files/covers/dark-mode.png'
WHERE slug = 'dark-mode-and-reading-experience';

UPDATE article SET cover_image = '/files/covers/retrospective.png'
WHERE slug = 'blog-system-project-retrospective';

UPDATE article SET cover_image = '/files/covers/writing-rhythm.png'
WHERE slug = 'writing-consistency-matters-more-than-time';

UPDATE article SET cover_image = '/files/covers/chase-value.png'
WHERE slug = 'i-no-longer-chase-impressive-things';
