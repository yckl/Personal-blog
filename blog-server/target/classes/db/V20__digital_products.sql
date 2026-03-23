-- J3: Digital product sales (eBook, courses, resource packs)

CREATE TABLE IF NOT EXISTS digital_product (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    slug            VARCHAR(200) NOT NULL UNIQUE,
    description     TEXT,
    cover_image     VARCHAR(500),
    product_type    VARCHAR(30)  NOT NULL DEFAULT 'EBOOK',
    price_cents     INT          NOT NULL DEFAULT 0,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'USD',
    file_url        VARCHAR(500),
    file_size_bytes BIGINT       DEFAULT 0,
    preview_url     VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    visibility      VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC',
    download_count  INT          NOT NULL DEFAULT 0,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_slug (slug),
    INDEX idx_type (product_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Purchase records
CREATE TABLE IF NOT EXISTS product_purchase (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id      BIGINT       NOT NULL,
    member_id       BIGINT       NOT NULL,
    order_no        VARCHAR(64)  NOT NULL,
    amount_cents    INT          NOT NULL,
    currency        VARCHAR(3)   NOT NULL DEFAULT 'USD',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PAID',
    download_token  VARCHAR(100),
    download_count  INT          NOT NULL DEFAULT 0,
    max_downloads   INT          NOT NULL DEFAULT 5,
    expires_at      DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product (product_id),
    INDEX idx_member (member_id),
    INDEX idx_order (order_no),
    UNIQUE KEY uk_product_member (product_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
