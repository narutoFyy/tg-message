CREATE TABLE push_device (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    device_token VARCHAR(255) NOT NULL,
    device_model VARCHAR(128),
    app_version VARCHAR(32),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_push_device_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uq_push_device_token UNIQUE (provider, device_token)
);
