-- I3: Audit log table + I4 schema additions

CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(50)  NOT NULL,
    resource    VARCHAR(50)  NOT NULL,
    resource_id BIGINT,
    detail      TEXT,
    ip_address  VARCHAR(50),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_action (action),
    INDEX idx_resource (resource, resource_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
