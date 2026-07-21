CREATE TABLE gift_card_rate_quote (
    id VARCHAR(36) PRIMARY KEY,
    rate_id VARCHAR(36) NOT NULL,
    face_currency_code VARCHAR(3) NOT NULL,
    local_payout_per_unit DECIMAL(18, 6) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_gift_card_quote_rate FOREIGN KEY (rate_id) REFERENCES gift_card_rate (id),
    CONSTRAINT ux_gift_card_quote_currency UNIQUE (rate_id, face_currency_code)
);

INSERT INTO gift_card_rate_quote (
    id,
    rate_id,
    face_currency_code,
    local_payout_per_unit,
    created_at,
    updated_at
)
SELECT
    id,
    id,
    'USD',
    local_payout_per_usd,
    updated_at,
    updated_at
FROM gift_card_rate
WHERE local_payout_per_usd IS NOT NULL
  AND local_payout_per_usd > 0;

CREATE TABLE gift_card_artwork (
    identity_key VARCHAR(133) PRIMARY KEY,
    image_url VARCHAR(500) NOT NULL,
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_gift_card_artwork_admin FOREIGN KEY (updated_by) REFERENCES app_user (id)
);
