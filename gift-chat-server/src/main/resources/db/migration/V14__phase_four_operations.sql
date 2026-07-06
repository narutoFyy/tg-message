ALTER TABLE trade_order
    ADD COLUMN cancel_reason VARCHAR(64);

ALTER TABLE trade_order
    ADD COLUMN cancel_note VARCHAR(255);

ALTER TABLE trade_order
    ADD COLUMN canceled_by_user_id VARCHAR(36);

ALTER TABLE trade_order
    ADD COLUMN canceled_at TIMESTAMP;

ALTER TABLE trade_order
    ADD CONSTRAINT fk_trade_order_canceled_by FOREIGN KEY (canceled_by_user_id) REFERENCES app_user (id);

ALTER TABLE broadcast_message
    ADD COLUMN country_codes VARCHAR(255);

ALTER TABLE broadcast_message
    ADD COLUMN search_keyword VARCHAR(128);

ALTER TABLE broadcast_message
    ADD COLUMN target_mode VARCHAR(32) DEFAULT 'FILTER';

ALTER TABLE broadcast_message
    ADD COLUMN target_usernames TEXT;

CREATE TABLE registration_bonus_config (
    id VARCHAR(36) PRIMARY KEY,
    country_code VARCHAR(8) NOT NULL UNIQUE,
    country_name VARCHAR(64) NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    bonus_amount DECIMAL(18, 2) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    note VARCHAR(255),
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_registration_bonus_config_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id)
);

CREATE TABLE registration_bonus_record (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    phone_snapshot VARCHAR(32),
    country_code VARCHAR(8),
    country_name VARCHAR(64),
    currency_code VARCHAR(16),
    bonus_amount DECIMAL(18, 2) NOT NULL,
    config_id VARCHAR(36),
    status_code VARCHAR(32) NOT NULL,
    reason_note VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_registration_bonus_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_registration_bonus_config FOREIGN KEY (config_id) REFERENCES registration_bonus_config (id),
    CONSTRAINT ux_registration_bonus_user UNIQUE (user_id)
);

INSERT INTO registration_bonus_config (
    id,
    country_code,
    country_name,
    currency_code,
    bonus_amount,
    enabled,
    note,
    updated_by,
    created_at,
    updated_at
) VALUES
    ('registration-bonus-ng', '+234', 'Nigeria', 'NGN', 2.00, TRUE, 'Default phase four registration bonus', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('registration-bonus-in', '+91', 'India', 'INR', 2.00, TRUE, 'Default phase four registration bonus', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('registration-bonus-gh', '+233', 'Ghana', 'GHS', 2.00, TRUE, 'Default phase four registration bonus', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
