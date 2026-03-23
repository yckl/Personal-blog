-- F1: Subscriber system enhancements

ALTER TABLE subscriber ADD COLUMN confirm_token VARCHAR(64) AFTER status;
ALTER TABLE subscriber ADD COLUMN source VARCHAR(50) DEFAULT 'website' AFTER confirm_token COMMENT 'website, article, footer, popup';
ALTER TABLE subscriber ADD COLUMN tags VARCHAR(500) AFTER source COMMENT 'Comma-separated preference tags';
ALTER TABLE subscriber ADD COLUMN confirmed_at DATETIME AFTER tags;

-- Update status values: PENDING → CONFIRMED → UNSUBSCRIBED
-- Existing ACTIVE subscribers should map to CONFIRMED
UPDATE subscriber SET status = 'CONFIRMED' WHERE status = 'ACTIVE';

ALTER TABLE subscriber ADD INDEX idx_status (status);
ALTER TABLE subscriber ADD INDEX idx_token (confirm_token);
