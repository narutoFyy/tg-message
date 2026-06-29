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
