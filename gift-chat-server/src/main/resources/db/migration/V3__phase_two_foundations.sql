CREATE TABLE IF NOT EXISTS app_notification (
    id VARCHAR(36) PRIMARY KEY,
    recipient_user_id VARCHAR(36) NOT NULL,
    actor_user_id VARCHAR(36),
    event_type VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    body VARCHAR(512) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    read_flag BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_notification_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS withdrawal_request (
    id VARCHAR(36) PRIMARY KEY,
    request_no VARCHAR(32) NOT NULL UNIQUE,
    owner_user_id VARCHAR(36) NOT NULL,
    assigned_agent_id VARCHAR(36),
    amount VARCHAR(32) NOT NULL,
    country VARCHAR(64) NOT NULL,
    account_name VARCHAR(128) NOT NULL,
    bank_name VARCHAR(128) NOT NULL,
    account_number VARCHAR(128) NOT NULL,
    contact VARCHAR(64),
    note VARCHAR(255),
    status_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_withdrawal_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_withdrawal_agent FOREIGN KEY (assigned_agent_id) REFERENCES app_user (id)
);
