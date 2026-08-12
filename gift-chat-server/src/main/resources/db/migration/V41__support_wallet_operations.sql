CREATE TABLE wallet_operation (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    operator_user_id VARCHAR(36) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    amount_delta DECIMAL(18, 2) NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    reference_id VARCHAR(64),
    note VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_wallet_operation_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_wallet_operation_operator FOREIGN KEY (operator_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_wallet_operation_user_created
    ON wallet_operation (user_id, created_at);
