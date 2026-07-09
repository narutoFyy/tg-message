CREATE TABLE agent_welcome_message (
    id VARCHAR(36) PRIMARY KEY,
    agent_user_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_agent_welcome_agent FOREIGN KEY (agent_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_agent_welcome_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id),
    CONSTRAINT ux_agent_welcome_agent UNIQUE (agent_user_id)
);

ALTER TABLE support_conversation
    ADD COLUMN welcome_message_sent_at TIMESTAMP;

ALTER TABLE support_conversation
    ADD COLUMN welcome_message_agent_id VARCHAR(36);

ALTER TABLE support_conversation
    ADD CONSTRAINT fk_support_welcome_agent FOREIGN KEY (welcome_message_agent_id) REFERENCES app_user (id);
