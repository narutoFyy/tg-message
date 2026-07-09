UPDATE registration_bonus_config
SET bonus_amount = 0.00,
    enabled = FALSE,
    note = 'Registration bonus disabled',
    updated_at = CURRENT_TIMESTAMP;

UPDATE registration_bonus_record
SET bonus_amount = 0.00,
    status_code = 'SKIPPED',
    reason_note = 'Registration bonus disabled'
WHERE status_code = 'AVAILABLE';

UPDATE referral_reward_config
SET registration_cashback_enabled = FALSE,
    registration_cashback_amount = 0.00,
    updated_at = CURRENT_TIMESTAMP;

UPDATE referral_reward
SET amount = 0.00,
    status_code = 'SKIPPED',
    updated_at = CURRENT_TIMESTAMP
WHERE reward_type = 'REGISTRATION'
  AND status_code = 'AVAILABLE';
