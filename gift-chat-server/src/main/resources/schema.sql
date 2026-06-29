CREATE TABLE IF NOT EXISTS app_user (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) UNIQUE,
    phone VARCHAR(32) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS gift_card_rate (
    id VARCHAR(36) PRIMARY KEY,
    card_name VARCHAR(128) NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    rate_value VARCHAR(64) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(36),
    CONSTRAINT fk_rate_updated_by FOREIGN KEY (updated_by) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS support_conversation (
    id VARCHAR(36) PRIMARY KEY,
    customer_user_id VARCHAR(36) NOT NULL,
    assigned_agent_id VARCHAR(36),
    assignment_status VARCHAR(32) NOT NULL,
    agent_note VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_support_customer FOREIGN KEY (customer_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_support_agent FOREIGN KEY (assigned_agent_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS support_message (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_user_id VARCHAR(36),
    sender_role VARCHAR(32) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
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
    status_code VARCHAR(32) NOT NULL,
    note VARCHAR(255),
    voucher_image_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_trade_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_trade_counterparty FOREIGN KEY (counterparty_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_trade_friendship FOREIGN KEY (friendship_id) REFERENCES friendship (id)
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
