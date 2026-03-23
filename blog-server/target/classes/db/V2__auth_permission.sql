-- =====================================================
-- V2: Auth & Permission Enhancements
-- =====================================================

-- 1. Add 2FA and login tracking fields to sys_user
ALTER TABLE sys_user
    ADD COLUMN two_factor_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN two_factor_secret  VARCHAR(255) DEFAULT NULL AFTER two_factor_enabled,
    ADD COLUMN last_login_at      DATETIME DEFAULT NULL AFTER two_factor_secret,
    ADD COLUMN last_login_ip      VARCHAR(50) DEFAULT NULL AFTER last_login_at;

-- 2. Permission table
CREATE TABLE sys_permission (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(100) NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(255),
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Role-Permission join table
CREATE TABLE sys_role_permission (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT       NOT NULL,
    permission_id BIGINT       NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Add SUPER_ADMIN role
INSERT INTO sys_role (role_name, role_key, description) VALUES
('Super Administrator', 'SUPER_ADMIN', 'Full system access including user management');

-- 5. Seed permissions
INSERT INTO sys_permission (code, name, description) VALUES
('article:view',    'View Articles',    'Can view all articles'),
('article:edit',    'Edit Articles',    'Can create and edit articles'),
('article:publish', 'Publish Articles', 'Can publish or unpublish articles'),
('comment:review',  'Review Comments',  'Can approve or reject comments'),
('media:upload',    'Upload Media',     'Can upload media files'),
('site:settings',   'Site Settings',    'Can modify site configuration'),
('user:manage',     'Manage Users',     'Can manage user accounts and roles');

-- 6. Assign permissions to roles
-- SUPER_ADMIN (id=4) gets ALL permissions
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE role_key = 'SUPER_ADMIN'), id FROM sys_permission;

-- ADMIN (id=1) gets all except user:manage
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE role_key = 'ADMIN'), id FROM sys_permission WHERE code != 'user:manage';

-- EDITOR (id=2) gets content permissions
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE role_key = 'EDITOR'), id FROM sys_permission
WHERE code IN ('article:view', 'article:edit', 'article:publish', 'comment:review', 'media:upload');

-- AUTHOR (id=3) gets basic content permissions
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT (SELECT id FROM sys_role WHERE role_key = 'AUTHOR'), id FROM sys_permission
WHERE code IN ('article:view', 'article:edit', 'media:upload');
