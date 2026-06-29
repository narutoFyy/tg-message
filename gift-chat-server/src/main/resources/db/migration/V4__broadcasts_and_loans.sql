CREATE TABLE IF NOT EXISTS broadcast_message (
    id VARCHAR(36) PRIMARY KEY,
    sender_user_id VARCHAR(36) NOT NULL,
    sender_role VARCHAR(32) NOT NULL,
    scope_code VARCHAR(32) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    delivered_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_broadcast_sender FOREIGN KEY (sender_user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS loan_application (
    id VARCHAR(36) PRIMARY KEY,
    application_no VARCHAR(32) NOT NULL UNIQUE,
    owner_user_id VARCHAR(36) NOT NULL,
    assigned_agent_id VARCHAR(36),
    amount VARCHAR(32) NOT NULL,
    country VARCHAR(64) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    contact VARCHAR(64),
    repayment_plan VARCHAR(255),
    status_code VARCHAR(32) NOT NULL,
    review_note VARCHAR(255),
    reviewed_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_loan_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_loan_agent FOREIGN KEY (assigned_agent_id) REFERENCES app_user (id),
    CONSTRAINT fk_loan_reviewer FOREIGN KEY (reviewed_by) REFERENCES app_user (id)
);
