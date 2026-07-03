ALTER TABLE app_user
    ADD COLUMN invite_code VARCHAR(32);

ALTER TABLE app_user
    ADD COLUMN referred_by_user_id VARCHAR(36);

ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_referred_by FOREIGN KEY (referred_by_user_id) REFERENCES app_user (id);

CREATE UNIQUE INDEX ux_app_user_invite_code ON app_user (invite_code);

UPDATE app_user
SET invite_code = UPPER(SUBSTRING(REPLACE(id, '-', ''), 1, 8))
WHERE invite_code IS NULL;

CREATE TABLE referral_reward_config (
    id VARCHAR(32) PRIMARY KEY,
    registration_cashback_enabled BOOLEAN NOT NULL,
    registration_cashback_amount DECIMAL(18, 2) NOT NULL,
    trade_rebate_enabled BOOLEAN NOT NULL,
    trade_rebate_percent DECIMAL(8, 4) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(36),
    CONSTRAINT fk_referral_config_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id)
);

INSERT INTO referral_reward_config (
    id,
    registration_cashback_enabled,
    registration_cashback_amount,
    trade_rebate_enabled,
    trade_rebate_percent,
    updated_at,
    updated_by
)
VALUES ('default', TRUE, 1.00, TRUE, 5.0000, CURRENT_TIMESTAMP, NULL);

CREATE TABLE referral_reward (
    id VARCHAR(36) PRIMARY KEY,
    referrer_user_id VARCHAR(36) NOT NULL,
    referred_user_id VARCHAR(36) NOT NULL,
    trade_order_id VARCHAR(36),
    source_key VARCHAR(64) NOT NULL,
    reward_type VARCHAR(32) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    rate_percent DECIMAL(8, 4),
    status_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_referral_reward_referrer FOREIGN KEY (referrer_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_referral_reward_referred FOREIGN KEY (referred_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_referral_reward_trade FOREIGN KEY (trade_order_id) REFERENCES trade_order (id)
);

CREATE UNIQUE INDEX ux_referral_reward_source ON referral_reward (reward_type, source_key);
