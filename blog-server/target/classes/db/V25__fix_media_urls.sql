-- Update media file URLs to use fast Chinese CDN sources

-- Images: use bing.com/th which is fast in China
UPDATE media_file SET file_url = 'https://cn.bing.com/th?id=OHR.MountRainier_ZH-CN5765943779_1920x1080.jpg' WHERE stored_name = 'img01.jpg';
UPDATE media_file SET file_url = 'https://cn.bing.com/th?id=OHR.AmboseliBiosworlds_ZH-CN5985168969_1920x1080.jpg' WHERE stored_name = 'img02.jpg';
UPDATE media_file SET file_url = 'https://cn.bing.com/th?id=OHR.MalschwitzPond_ZH-CN6178547808_1920x1080.jpg' WHERE stored_name = 'img03.jpg';
UPDATE media_file SET file_url = 'https://cn.bing.com/th?id=OHR.ParitutuRock_ZH-CN6412266488_1920x1080.jpg' WHERE stored_name = 'img04.jpg';
UPDATE media_file SET file_url = 'https://cn.bing.com/th?id=OHR.DesertDunes_ZH-CN6620302696_1920x1080.jpg' WHERE stored_name = 'img05.jpg';

-- Videos: use a reliable video sample source
UPDATE media_file SET file_url = 'https://vjs.zencdn.net/v/oceans.mp4' WHERE stored_name = 'vid01.mp4';
UPDATE media_file SET file_url = 'https://vjs.zencdn.net/v/oceans.mp4' WHERE stored_name = 'vid02.mp4';
UPDATE media_file SET file_url = 'https://vjs.zencdn.net/v/oceans.mp4' WHERE stored_name = 'vid03.mp4';
UPDATE media_file SET file_url = 'https://vjs.zencdn.net/v/oceans.mp4' WHERE stored_name = 'vid04.mp4';
UPDATE media_file SET file_url = 'https://vjs.zencdn.net/v/oceans.mp4' WHERE stored_name = 'vid05.mp4';

-- Audio: use a proper audio sample
UPDATE media_file SET file_url = 'https://file-examples.com/storage/feaade38c1679c47c0977c5/2017/11/file_example_MP3_700KB.mp3' WHERE stored_name = 'aud01.mp3';
UPDATE media_file SET file_url = 'https://file-examples.com/storage/feaade38c1679c47c0977c5/2017/11/file_example_MP3_700KB.mp3' WHERE stored_name = 'aud02.mp3';
UPDATE media_file SET file_url = 'https://file-examples.com/storage/feaade38c1679c47c0977c5/2017/11/file_example_MP3_700KB.mp3' WHERE stored_name = 'aud03.mp3';
UPDATE media_file SET file_url = 'https://file-examples.com/storage/feaade38c1679c47c0977c5/2017/11/file_example_MP3_700KB.mp3' WHERE stored_name = 'aud04.mp3';
UPDATE media_file SET file_url = 'https://file-examples.com/storage/feaade38c1679c47c0977c5/2017/11/file_example_MP3_700KB.mp3' WHERE stored_name = 'aud05.mp3';

-- Digital products cover images
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.FreshwaterHouses_ZH-CN7039498018_1920x1080.jpg' WHERE slug = 'vue-3-advanced';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.JericoacoaraFP_ZH-CN7206085654_1920x1080.jpg' WHERE slug = 'react-design-patterns';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.StellarJays_ZH-CN7446735443_1920x1080.jpg' WHERE slug = 'fullstack-journey';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.TanzaniaBeeEater_ZH-CN7708486982_1920x1080.jpg' WHERE slug = 'css-for-backend';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.KalsoyFaroe_ZH-CN8044024497_1920x1080.jpg' WHERE slug = 'interview-guide';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.BluebellForest_ZH-CN8280255299_1920x1080.jpg' WHERE slug = 'mastering-spring-boot';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.MudijDogs_ZH-CN8450539442_1920x1080.jpg' WHERE slug = 'vue3-masterclass';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.CoveSpires_ZH-CN8651698096_1920x1080.jpg' WHERE slug = 'db-tuning-course';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.MountBaldy_ZH-CN8879747474_1920x1080.jpg' WHERE slug = 'figma-devs-course';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.PerchTrondheim_ZH-CN9054804131_1920x1080.jpg' WHERE slug = 'devops-101';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.JavelinsEdge_ZH-CN9275519752_1920x1080.jpg' WHERE slug = 'premium-ui-kit';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.Jondal_ZH-CN9458757117_1920x1080.jpg' WHERE slug = 'icon-set-minimal';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.KayakAntworthy_ZH-CN9672825793_1920x1080.jpg' WHERE slug = 'notion-templates-pack';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.AnteloperIsland_ZH-CN9899449107_1920x1080.jpg' WHERE slug = 'resume-templates';
UPDATE digital_product SET cover_image = 'https://cn.bing.com/th?id=OHR.SiblingBears_ZH-CN0196783499_1920x1080.jpg' WHERE slug = 'bg-textures-pack';

-- Update digital product prices to CNY
UPDATE digital_product SET currency = 'CNY', price_cents = price_cents * 7 WHERE currency = 'USD';
UPDATE digital_product SET price_cents = 0 WHERE slug IN ('fullstack-journey', 'resume-templates');

-- Fix menu items: V24 used wrong column name 'route' instead of 'url'
-- Delete any bad menu data and re-insert correctly
DELETE FROM menu_config WHERE name IN ('Home', 'Articles', 'Archive', 'About', 'Contact');
INSERT INTO menu_config (name, url, icon, sort_order, parent_id, visible, is_external) VALUES
('首页', '/', 'HomeFilled', 10, 0, TRUE, FALSE),
('文章', '/articles', 'List', 20, 0, TRUE, FALSE),
('归档', '/archive', 'Calendar', 30, 0, TRUE, FALSE),
('关于', '/about', 'User', 40, 0, TRUE, FALSE),
('数字工坊', '/products', 'ShoppingBag', 45, 0, TRUE, FALSE),
('联系', '/contact', 'Message', 50, 0, TRUE, FALSE);

