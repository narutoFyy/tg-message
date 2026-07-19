CREATE TABLE trade_order_number (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    allocated_at TIMESTAMP NOT NULL
);

ALTER TABLE trade_order ADD COLUMN client_request_id VARCHAR(64);
ALTER TABLE trade_order ADD COLUMN client_request_hash VARCHAR(64);

CREATE UNIQUE INDEX ux_trade_order_owner_request
    ON trade_order(owner_user_id, client_request_id);

ALTER TABLE gift_card_rate ADD COLUMN identity_key VARCHAR(133);

UPDATE gift_card_rate
SET identity_key = CASE
    WHEN card_code IS NOT NULL AND TRIM(card_code) <> ''
        THEN CONCAT('CODE:', UPPER(TRIM(card_code)))
    ELSE CONCAT('NAME:', LOWER(TRIM(card_name)))
END;

CREATE UNIQUE INDEX ux_gift_card_rate_region_identity
    ON gift_card_rate(region_code, identity_key);

INSERT INTO agent_welcome_message (
    id,
    agent_user_id,
    content,
    enabled,
    updated_by,
    created_at,
    updated_at
)
SELECT
    agent.id,
    agent.id,
    'Hello! Welcome to Xcard. I am your dedicated support agent. How can I help you today?',
    TRUE,
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM app_user agent
WHERE UPPER(agent.role_code) = 'AGENT'
  AND UPPER(agent.status_code) = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM agent_welcome_message welcome
      WHERE welcome.agent_user_id = agent.id
  );
