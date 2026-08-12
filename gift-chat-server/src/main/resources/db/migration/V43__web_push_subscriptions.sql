CREATE TABLE web_push_subscription (
    id VARCHAR(68) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    endpoint VARCHAR(2048) NOT NULL,
    p256dh_key VARCHAR(255) NOT NULL,
    auth_key VARCHAR(255) NOT NULL,
    user_agent VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_web_push_subscription_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_web_push_subscription_user_enabled
    ON web_push_subscription (user_id, enabled);
