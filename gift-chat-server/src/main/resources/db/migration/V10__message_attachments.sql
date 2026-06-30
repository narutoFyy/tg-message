CREATE TABLE IF NOT EXISTS message_attachment (
    id VARCHAR(64) PRIMARY KEY,
    owner_message_type VARCHAR(32) NOT NULL,
    owner_message_id VARCHAR(64) NOT NULL,
    attachment_type VARCHAR(32) NOT NULL,
    asset_id VARCHAR(64),
    url VARCHAR(512) NOT NULL,
    thumbnail_url VARCHAR(512),
    mime_type VARCHAR(100),
    original_name VARCHAR(255),
    size_bytes BIGINT,
    width INT,
    height INT,
    duration_ms BIGINT,
    sort_order INT NOT NULL DEFAULT 0,
    status_code VARCHAR(32) NOT NULL DEFAULT 'READY',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_message_attachment_asset FOREIGN KEY (asset_id) REFERENCES upload_asset (id)
);

CREATE INDEX IF NOT EXISTS idx_message_attachment_owner
ON message_attachment (owner_message_type, owner_message_id, sort_order);
