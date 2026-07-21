ALTER TABLE app_user ADD COLUMN birth_date DATE;

CREATE TABLE vip_benefit_config (
    id VARCHAR(36) PRIMARY KEY,
    vip4_support_amount_ngn DECIMAL(18, 2) NOT NULL,
    vip5_support_amount_ngn DECIMAL(18, 2) NOT NULL,
    support_reward_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_vip_benefit_config_admin FOREIGN KEY (updated_by) REFERENCES app_user (id)
);
CREATE TABLE vip_holiday_reward (
    id VARCHAR(36) PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL,
    holiday_code VARCHAR(64) NOT NULL,
    holiday_name VARCHAR(128) NOT NULL,
    holiday_date DATE NOT NULL,
    reward_amount DECIMAL(18, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_vip_holiday_admin FOREIGN KEY (updated_by) REFERENCES app_user (id),
    CONSTRAINT ux_vip_holiday UNIQUE (country_code, holiday_code, holiday_date)
);

CREATE TABLE vip_benefit_claim (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    holiday_reward_id VARCHAR(36),
    benefit_type VARCHAR(32) NOT NULL,
    source_key VARCHAR(160) NOT NULL,
    period_key VARCHAR(32) NOT NULL,
    vip_level VARCHAR(16) NOT NULL,
    status_code VARCHAR(16) NOT NULL,
    base_amount_usd DECIMAL(18, 6) NOT NULL,
    local_amount DECIMAL(18, 2) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    exchange_rate_snapshot DECIMAL(18, 6) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reviewed_by VARCHAR(36),
    reviewed_at TIMESTAMP,
    review_note VARCHAR(255),
    CONSTRAINT fk_vip_benefit_claim_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_vip_benefit_claim_holiday FOREIGN KEY (holiday_reward_id) REFERENCES vip_holiday_reward (id),
    CONSTRAINT fk_vip_benefit_claim_reviewer FOREIGN KEY (reviewed_by) REFERENCES app_user (id),
    CONSTRAINT ux_vip_benefit_claim_source UNIQUE (source_key)
);

INSERT INTO vip_benefit_config (
    id,
    vip4_support_amount_ngn,
    vip5_support_amount_ngn,
    support_reward_enabled,
    created_at,
    updated_at
) VALUES (
    'vip-benefit-default',
    0,
    0,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
