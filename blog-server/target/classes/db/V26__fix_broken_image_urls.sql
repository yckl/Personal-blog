-- Fix broken Bing image URLs (404) with working picsum.photos URLs

-- Media file images
UPDATE media_file SET file_url = 'https://picsum.photos/seed/mountain1/1920/1080' WHERE stored_name = 'img01.jpg';
UPDATE media_file SET file_url = 'https://picsum.photos/seed/river2/1920/1080' WHERE stored_name = 'img02.jpg';
UPDATE media_file SET file_url = 'https://picsum.photos/seed/city3/1920/1080' WHERE stored_name = 'img03.jpg';
UPDATE media_file SET file_url = 'https://picsum.photos/seed/forest4/1920/1080' WHERE stored_name = 'img04.jpg';
UPDATE media_file SET file_url = 'https://picsum.photos/seed/desert5/1920/1080' WHERE stored_name = 'img05.jpg';

-- Digital product cover images
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/vue3adv/800/600' WHERE slug = 'vue-3-advanced';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/reactpat/800/600' WHERE slug = 'react-design-patterns';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/fullstk/800/600' WHERE slug = 'fullstack-journey';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/cssback/800/600' WHERE slug = 'css-for-backend';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/intervw/800/600' WHERE slug = 'interview-guide';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/springbt/800/600' WHERE slug = 'mastering-spring-boot';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/vue3mc/800/600' WHERE slug = 'vue3-masterclass';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/dbtune/800/600' WHERE slug = 'db-tuning-course';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/figmadev/800/600' WHERE slug = 'figma-devs-course';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/devops1/800/600' WHERE slug = 'devops-101';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/uikit1/800/600' WHERE slug = 'premium-ui-kit';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/iconset/800/600' WHERE slug = 'icon-set-minimal';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/notion1/800/600' WHERE slug = 'notion-templates-pack';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/resume1/800/600' WHERE slug = 'resume-templates';
UPDATE digital_product SET cover_image = 'https://picsum.photos/seed/bgtex1/800/600' WHERE slug = 'bg-textures-pack';
