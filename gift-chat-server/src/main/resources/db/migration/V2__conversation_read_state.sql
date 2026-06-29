CREATE TABLE IF NOT EXISTS conversation_read_state (
    id VARCHAR(36) PRIMARY KEY,
    conversation_type VARCHAR(32) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    last_read_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversation_read_state_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uq_conversation_read_state UNIQUE (conversation_type, conversation_id, user_id)
);
