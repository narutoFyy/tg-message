ALTER TABLE trade_order ADD COLUMN estimated_local_amount DECIMAL(18, 2);
ALTER TABLE trade_order ADD COLUMN final_local_amount DECIMAL(18, 2);
ALTER TABLE trade_order ADD COLUMN manual_vip_points DECIMAL(18, 2);
ALTER TABLE trade_order ADD COLUMN settlement_reason VARCHAR(255);
ALTER TABLE trade_order ADD COLUMN settled_by_user_id VARCHAR(36);
ALTER TABLE trade_order ADD COLUMN settled_at TIMESTAMP;

UPDATE trade_order
SET estimated_local_amount = local_amount
WHERE estimated_local_amount IS NULL;

UPDATE trade_order
SET final_local_amount = local_amount,
    manual_vip_points = 0
WHERE UPPER(status_code) = 'COMPLETED'
  AND final_local_amount IS NULL;

ALTER TABLE trade_order
    ADD CONSTRAINT fk_trade_order_settled_by FOREIGN KEY (settled_by_user_id) REFERENCES app_user (id);

ALTER TABLE support_message ADD COLUMN trade_order_id VARCHAR(36);
ALTER TABLE support_message
    ADD CONSTRAINT fk_support_message_trade_order FOREIGN KEY (trade_order_id) REFERENCES trade_order (id);

CREATE UNIQUE INDEX ux_support_message_trade_order ON support_message (trade_order_id);

CREATE TABLE trade_order_settlement_audit (
    id VARCHAR(36) PRIMARY KEY,
    trade_order_id VARCHAR(36) NOT NULL,
    operator_user_id VARCHAR(36) NOT NULL,
    action_code VARCHAR(32) NOT NULL,
    estimated_local_amount DECIMAL(18, 2),
    final_local_amount DECIMAL(18, 2),
    currency_code VARCHAR(3),
    vip_points DECIMAL(18, 2) NOT NULL,
    reason_note VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_trade_settlement_audit_order FOREIGN KEY (trade_order_id) REFERENCES trade_order (id),
    CONSTRAINT fk_trade_settlement_audit_operator FOREIGN KEY (operator_user_id) REFERENCES app_user (id)
);

CREATE UNIQUE INDEX ux_trade_settlement_audit_action
    ON trade_order_settlement_audit (trade_order_id, action_code);
