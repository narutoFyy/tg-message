ALTER TABLE app_user
    ADD COLUMN onboarding_policy_version INT NOT NULL DEFAULT 0;

ALTER TABLE app_user
    ADD COLUMN lottery_access_status VARCHAR(16) NOT NULL DEFAULT 'GRANDFATHERED';

ALTER TABLE app_user
    ADD COLUMN lottery_approved_by_user_id VARCHAR(36);

ALTER TABLE app_user
    ADD COLUMN lottery_approved_at TIMESTAMP;

ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_lottery_approved_by
        FOREIGN KEY (lottery_approved_by_user_id) REFERENCES app_user (id);

ALTER TABLE registration_bonus_record
    ADD COLUMN unlocked_by_order_id VARCHAR(36);

ALTER TABLE registration_bonus_record
    ADD COLUMN unlocked_at TIMESTAMP;

ALTER TABLE registration_bonus_record
    ADD CONSTRAINT fk_registration_bonus_unlock_order
        FOREIGN KEY (unlocked_by_order_id) REFERENCES trade_order (id);

UPDATE registration_bonus_config
SET bonus_amount = 2000.00,
    currency_code = 'NGN',
    enabled = TRUE,
    note = 'Nigeria welcome bonus; locked until first completed support sell order',
    updated_at = CURRENT_TIMESTAMP
WHERE country_code = '+234';
