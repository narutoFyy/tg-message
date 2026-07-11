ALTER TABLE withdrawal_request
    ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'WALLET';

UPDATE withdrawal_request
SET source_type = 'LOTTERY_CASH'
WHERE lottery_draw_record_id IS NOT NULL;

CREATE INDEX idx_withdrawal_owner_source_status
    ON withdrawal_request (owner_user_id, source_type, status_code);

CREATE TABLE lottery_fulfillment_order (
    id VARCHAR(36) PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    owner_user_id VARCHAR(36) NOT NULL,
    assigned_agent_id VARCHAR(36),
    lottery_draw_record_id VARCHAR(36) NOT NULL,
    recipient_name VARCHAR(128) NOT NULL,
    phone VARCHAR(64) NOT NULL,
    country VARCHAR(64) NOT NULL,
    address_line VARCHAR(512) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lottery_fulfillment_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_lottery_fulfillment_agent FOREIGN KEY (assigned_agent_id) REFERENCES app_user (id),
    CONSTRAINT fk_lottery_fulfillment_draw FOREIGN KEY (lottery_draw_record_id) REFERENCES lottery_draw_record (id),
    CONSTRAINT ux_lottery_fulfillment_draw UNIQUE (lottery_draw_record_id)
);

CREATE INDEX idx_lottery_fulfillment_agent_status
    ON lottery_fulfillment_order (assigned_agent_id, status_code, updated_at);
