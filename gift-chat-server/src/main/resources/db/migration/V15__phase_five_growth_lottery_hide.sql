CREATE TABLE vip_point_ledger (
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

CREATE TABLE lottery_prize (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    prize_type VARCHAR(32) NOT NULL,
    weight_value INT NOT NULL,
    image_url VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE lottery_draw_record (
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
    CONSTRAINT fk_lottery_draw_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_lottery_draw_prize FOREIGN KEY (prize_id) REFERENCES lottery_prize (id),
    CONSTRAINT fk_lottery_draw_processed_by FOREIGN KEY (processed_by) REFERENCES app_user (id),
    CONSTRAINT ux_lottery_draw_period UNIQUE (user_id, period_type, period_key)
);

CREATE TABLE lottery_eligibility_reset (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    admin_user_id VARCHAR(36) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_lottery_reset_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_lottery_reset_admin FOREIGN KEY (admin_user_id) REFERENCES app_user (id)
);

CREATE TABLE user_hidden_record (
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

INSERT INTO lottery_prize (
    id,
    name,
    prize_type,
    weight_value,
    image_url,
    enabled,
    sort_order,
    created_at,
    updated_at
) VALUES
    ('lottery-prize-ngn-1000', '₦1000', 'CASH', 6, '', TRUE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('lottery-prize-ngn-2000', '₦2000', 'CASH', 7, '', TRUE, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('lottery-prize-ngn-3000', '₦3000', 'CASH', 10, '', TRUE, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('lottery-prize-ngn-5000', '₦5000', 'CASH', 10, '', TRUE, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('lottery-prize-ngn-8000', '₦8000', 'CASH', 9, '', TRUE, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('lottery-prize-ngn-10000', '₦10000', 'CASH', 8, '', TRUE, 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('lottery-prize-ngn-15000', '₦15000', 'CASH', 7, '', TRUE, 70, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('lottery-prize-ipad', 'iPad', 'PHYSICAL', 6, '/static/lottery/ipad.jpg', TRUE, 80, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('lottery-prize-iphone-17', 'iPhone 17', 'PHYSICAL', 6, '/static/lottery/iphone.jpg', TRUE, 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('lottery-prize-computer', 'Computer', 'PHYSICAL', 6, '/static/lottery/diannao.jpg', TRUE, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
