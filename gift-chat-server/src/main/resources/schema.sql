CREATE TABLE IF NOT EXISTS app_user (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) UNIQUE,
    phone VARCHAR(32) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    tencent_user_id VARCHAR(32) UNIQUE,
    avatar_url VARCHAR(255),
    invite_code VARCHAR(32) UNIQUE,
    referred_by_user_id VARCHAR(36),
    country_code VARCHAR(2),
    currency_code VARCHAR(3),
    country_binding_status VARCHAR(16),
    country_bound_at TIMESTAMP,
    country_bound_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_app_user_referred_by FOREIGN KEY (referred_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_app_user_country_bound_by FOREIGN KEY (country_bound_by) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS gift_card_rate (
    id VARCHAR(36) PRIMARY KEY,
    card_name VARCHAR(128) NOT NULL,
    card_code VARCHAR(64),
    identity_key VARCHAR(133),
    region_code VARCHAR(32) NOT NULL,
    rate_value VARCHAR(64) NOT NULL,
    currency_code VARCHAR(3),
    local_payout_per_usd DECIMAL(18, 6),
    status_code VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(36),
    CONSTRAINT fk_rate_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id),
    CONSTRAINT ux_gift_card_rate_region_identity UNIQUE (region_code, identity_key)
);

CREATE TABLE IF NOT EXISTS support_conversation (
    id VARCHAR(36) PRIMARY KEY,
    customer_user_id VARCHAR(36) NOT NULL,
    assigned_agent_id VARCHAR(36),
    assignment_status VARCHAR(32) NOT NULL,
    agent_note VARCHAR(255),
    welcome_message_sent_at TIMESTAMP,
    welcome_message_agent_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_support_customer FOREIGN KEY (customer_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_support_agent FOREIGN KEY (assigned_agent_id) REFERENCES app_user (id),
    CONSTRAINT fk_support_welcome_agent FOREIGN KEY (welcome_message_agent_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS currency_exchange_rate (
    id VARCHAR(36) PRIMARY KEY,
    country_code VARCHAR(2) NOT NULL UNIQUE,
    currency_code VARCHAR(3) NOT NULL,
    local_currency_per_usd DECIMAL(18, 6) NOT NULL,
    enabled BOOLEAN NOT NULL,
    note VARCHAR(255),
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_currency_rate_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS support_message (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_user_id VARCHAR(36),
    sender_role VARCHAR(32) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    client_message_id VARCHAR(64),
    reply_to_message_id VARCHAR(36),
    reply_to_author VARCHAR(32),
    reply_to_content TEXT,
    server_seq BIGINT NOT NULL,
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED',
    delivered_at TIMESTAMP,
    failed_reason VARCHAR(255),
    tencent_mirror_status VARCHAR(32) NOT NULL DEFAULT 'SKIPPED',
    tencent_message_key VARCHAR(128),
    tencent_mirrored_at TIMESTAMP,
    tencent_mirror_error VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_support_message_conversation FOREIGN KEY (conversation_id) REFERENCES support_conversation (id),
    CONSTRAINT fk_support_message_sender FOREIGN KEY (sender_user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS conversation_read_state (
    id VARCHAR(36) PRIMARY KEY,
    conversation_type VARCHAR(32) NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    last_read_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_conversation_read_state_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uq_conversation_read_state UNIQUE (conversation_type, conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS friendship (
    id VARCHAR(36) PRIMARY KEY,
    requester_user_id VARCHAR(36) NOT NULL,
    addressee_user_id VARCHAR(36) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_friend_requester FOREIGN KEY (requester_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_friend_addressee FOREIGN KEY (addressee_user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS direct_message (
    id VARCHAR(36) PRIMARY KEY,
    friendship_id VARCHAR(36) NOT NULL,
    sender_user_id VARCHAR(36) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    client_message_id VARCHAR(64),
    server_seq BIGINT NOT NULL,
    delivery_status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED',
    delivered_at TIMESTAMP,
    failed_reason VARCHAR(255),
    tencent_mirror_status VARCHAR(32) NOT NULL DEFAULT 'SKIPPED',
    tencent_message_key VARCHAR(128),
    tencent_mirrored_at TIMESTAMP,
    tencent_mirror_error VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_direct_message_friendship FOREIGN KEY (friendship_id) REFERENCES friendship (id),
    CONSTRAINT fk_direct_message_sender FOREIGN KEY (sender_user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS trade_order (
    id VARCHAR(36) PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    owner_user_id VARCHAR(36) NOT NULL,
    counterparty_user_id VARCHAR(36) NOT NULL,
    friendship_id VARCHAR(36),
    card_name VARCHAR(128) NOT NULL,
    face_value VARCHAR(32) NOT NULL,
    payout_amount VARCHAR(32) NOT NULL,
    base_amount_usd DECIMAL(18, 6),
    local_amount DECIMAL(18, 2),
    currency_code VARCHAR(3),
    business_rate_snapshot DECIMAL(18, 6),
    client_request_id VARCHAR(64),
    client_request_hash VARCHAR(64),
    status_code VARCHAR(32) NOT NULL,
    note VARCHAR(255),
    voucher_image_url VARCHAR(255),
    cancel_reason VARCHAR(64),
    cancel_note VARCHAR(255),
    canceled_by_user_id VARCHAR(36),
    canceled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_trade_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_trade_counterparty FOREIGN KEY (counterparty_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_trade_friendship FOREIGN KEY (friendship_id) REFERENCES friendship (id),
    CONSTRAINT fk_trade_order_canceled_by FOREIGN KEY (canceled_by_user_id) REFERENCES app_user (id),
    CONSTRAINT ux_trade_order_owner_request UNIQUE (owner_user_id, client_request_id)
);

CREATE TABLE IF NOT EXISTS trade_order_number (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    allocated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS agent_welcome_message (
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

CREATE TABLE IF NOT EXISTS upload_asset (
    id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    public_url VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_upload_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS blacklist_entry (
    id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    blocked_user_id VARCHAR(36) NOT NULL,
    blocked_phone_snapshot VARCHAR(32) NOT NULL,
    reason_note VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_blacklist_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_blacklist_blocked FOREIGN KEY (blocked_user_id) REFERENCES app_user (id)
);

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

CREATE TABLE IF NOT EXISTS push_device (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    device_token VARCHAR(255) NOT NULL,
    device_model VARCHAR(128),
    app_version VARCHAR(32),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_push_device_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uq_push_device_token UNIQUE (provider, device_token)
);

CREATE TABLE IF NOT EXISTS user_bank_account (
    id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    country VARCHAR(64) NOT NULL,
    account_name VARCHAR(128) NOT NULL,
    bank_name VARCHAR(128) NOT NULL,
    account_number VARCHAR(128) NOT NULL,
    normalized_bank_name VARCHAR(128) NOT NULL,
    normalized_account_number VARCHAR(128) NOT NULL,
    account_fingerprint VARCHAR(64) NOT NULL,
    masked_account_number VARCHAR(64) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_bank_account_user FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT ux_user_bank_account_user UNIQUE (owner_user_id),
    CONSTRAINT ux_user_bank_account_fingerprint UNIQUE (account_fingerprint)
);

CREATE TABLE IF NOT EXISTS withdrawal_request (
    id VARCHAR(36) PRIMARY KEY,
    request_no VARCHAR(32) NOT NULL UNIQUE,
    owner_user_id VARCHAR(36) NOT NULL,
    assigned_agent_id VARCHAR(36),
    bank_account_id VARCHAR(36),
    lottery_draw_record_id VARCHAR(36),
    source_type VARCHAR(32) NOT NULL DEFAULT 'WALLET',
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
    CONSTRAINT fk_withdrawal_agent FOREIGN KEY (assigned_agent_id) REFERENCES app_user (id),
    CONSTRAINT fk_withdrawal_bank_account FOREIGN KEY (bank_account_id) REFERENCES user_bank_account (id),
    CONSTRAINT fk_withdrawal_lottery_draw FOREIGN KEY (lottery_draw_record_id) REFERENCES lottery_draw_record (id)
);

CREATE TABLE IF NOT EXISTS lottery_fulfillment_order (
    id VARCHAR(36) PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    owner_user_id VARCHAR(36) NOT NULL,
    assigned_agent_id VARCHAR(36),
    lottery_draw_record_id VARCHAR(36) NOT NULL UNIQUE,
    recipient_name VARCHAR(128) NOT NULL,
    phone VARCHAR(64) NOT NULL,
    country VARCHAR(64) NOT NULL,
    address_line VARCHAR(512) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lottery_fulfillment_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_lottery_fulfillment_agent FOREIGN KEY (assigned_agent_id) REFERENCES app_user (id),
    CONSTRAINT fk_lottery_fulfillment_draw FOREIGN KEY (lottery_draw_record_id) REFERENCES lottery_draw_record (id)
);

CREATE TABLE IF NOT EXISTS broadcast_message (
    id VARCHAR(36) PRIMARY KEY,
    sender_user_id VARCHAR(36) NOT NULL,
    sender_role VARCHAR(32) NOT NULL,
    scope_code VARCHAR(32) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    media_url VARCHAR(512),
    delivered_count INT NOT NULL,
    country_codes VARCHAR(255),
    search_keyword VARCHAR(128),
    target_mode VARCHAR(32) DEFAULT 'FILTER',
    target_usernames TEXT,
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

CREATE TABLE IF NOT EXISTS video_session (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(64) NOT NULL UNIQUE,
    channel_type VARCHAR(32) NOT NULL,
    channel_id VARCHAR(64) NOT NULL,
    initiator_user_id VARCHAR(36) NOT NULL,
    receiver_user_id VARCHAR(36) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    vendor_code VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_video_initiator FOREIGN KEY (initiator_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_video_receiver FOREIGN KEY (receiver_user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS referral_reward_config (
    id VARCHAR(32) PRIMARY KEY,
    registration_cashback_enabled BOOLEAN NOT NULL,
    registration_cashback_amount DECIMAL(18, 2) NOT NULL,
    trade_rebate_enabled BOOLEAN NOT NULL,
    trade_rebate_percent DECIMAL(8, 4) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(36),
    CONSTRAINT fk_referral_config_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS referral_reward (
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
    CONSTRAINT fk_referral_reward_trade FOREIGN KEY (trade_order_id) REFERENCES trade_order (id),
    CONSTRAINT ux_referral_reward_source UNIQUE (reward_type, source_key)
);

CREATE TABLE IF NOT EXISTS registration_bonus_config (
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

CREATE TABLE IF NOT EXISTS registration_bonus_record (
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

CREATE TABLE IF NOT EXISTS vip_point_ledger (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    trade_order_id VARCHAR(36),
    source_key VARCHAR(96) NOT NULL,
    points_delta DECIMAL(18, 2) NOT NULL,
    reason_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_vip_point_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_vip_point_trade FOREIGN KEY (trade_order_id) REFERENCES trade_order (id),
    CONSTRAINT ux_vip_point_source UNIQUE (source_key)
);

CREATE TABLE IF NOT EXISTS lottery_prize (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    prize_type VARCHAR(32) NOT NULL,
    weight_value INT NOT NULL,
    image_url VARCHAR(255),
    base_amount_usd DECIMAL(18, 6),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS lottery_draw_record (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    vip_level VARCHAR(16) NOT NULL,
    prize_id VARCHAR(36) NOT NULL,
    period_type VARCHAR(16) NOT NULL,
    period_key VARCHAR(32) NOT NULL,
    drawn_at TIMESTAMP NOT NULL,
    fulfillment_status VARCHAR(32) NOT NULL,
    processed_by VARCHAR(36),
    processed_at TIMESTAMP,
    base_amount_usd DECIMAL(18, 6),
    local_amount DECIMAL(18, 2),
    currency_code VARCHAR(3),
    exchange_rate_snapshot DECIMAL(18, 6),
    CONSTRAINT fk_lottery_draw_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_lottery_draw_prize FOREIGN KEY (prize_id) REFERENCES lottery_prize (id),
    CONSTRAINT fk_lottery_draw_processed_by FOREIGN KEY (processed_by) REFERENCES app_user (id),
    CONSTRAINT ux_lottery_draw_period UNIQUE (user_id, period_type, period_key)
);

CREATE TABLE IF NOT EXISTS lottery_eligibility_reset (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    admin_user_id VARCHAR(36) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lottery_reset_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_lottery_reset_admin FOREIGN KEY (admin_user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS user_hidden_record (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    hidden_scope VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    restored_at TIMESTAMP,
    CONSTRAINT fk_user_hidden_record_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT ux_user_hidden_record UNIQUE (user_id, target_type, target_id, hidden_scope)
);
