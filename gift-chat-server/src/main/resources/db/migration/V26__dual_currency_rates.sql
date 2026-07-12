ALTER TABLE gift_card_rate ADD COLUMN currency_code VARCHAR(3);
ALTER TABLE gift_card_rate ADD COLUMN local_payout_per_usd DECIMAL(18, 6);

UPDATE gift_card_rate
SET currency_code = CASE region_code
    WHEN 'NG' THEN 'NGN'
    WHEN 'IN' THEN 'INR'
    WHEN 'CM' THEN 'XAF'
    WHEN 'GH' THEN 'GHS'
    WHEN 'KE' THEN 'KES'
    WHEN 'US' THEN 'USD'
    ELSE NULL
END;

CREATE TABLE currency_exchange_rate (
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

INSERT INTO currency_exchange_rate (
    id, country_code, currency_code, local_currency_per_usd, enabled, note, updated_by, created_at, updated_at
) VALUES
    ('currency-rate-ng', 'NG', 'NGN', 1500.000000, TRUE, 'Initial administrator-managed rate; verify before production use', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('currency-rate-in', 'IN', 'INR', 83.000000, TRUE, 'Initial administrator-managed rate; verify before production use', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('currency-rate-cm', 'CM', 'XAF', 600.000000, TRUE, 'Initial administrator-managed rate; verify before production use', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('currency-rate-gh', 'GH', 'GHS', 15.000000, TRUE, 'Initial administrator-managed rate; verify before production use', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('currency-rate-ke', 'KE', 'KES', 130.000000, TRUE, 'Initial administrator-managed rate; verify before production use', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('currency-rate-us', 'US', 'USD', 1.000000, TRUE, 'USD reference rate', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
