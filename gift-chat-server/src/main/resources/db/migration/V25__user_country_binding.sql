ALTER TABLE app_user ADD COLUMN country_code VARCHAR(2);
ALTER TABLE app_user ADD COLUMN currency_code VARCHAR(3);
ALTER TABLE app_user ADD COLUMN country_binding_status VARCHAR(16);
ALTER TABLE app_user ADD COLUMN country_bound_at TIMESTAMP;
ALTER TABLE app_user ADD COLUMN country_bound_by VARCHAR(36);

ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_country_bound_by FOREIGN KEY (country_bound_by) REFERENCES app_user (id);

UPDATE app_user
SET country_code = CASE
        WHEN REPLACE(phone, ' ', '') LIKE '+234%' THEN 'NG'
        WHEN REPLACE(phone, ' ', '') LIKE '+91%' THEN 'IN'
        WHEN REPLACE(phone, ' ', '') LIKE '+237%' THEN 'CM'
        WHEN REPLACE(phone, ' ', '') LIKE '+233%' THEN 'GH'
        WHEN REPLACE(phone, ' ', '') LIKE '+254%' THEN 'KE'
        WHEN REPLACE(phone, ' ', '') LIKE '+1%' THEN 'US'
        ELSE NULL
    END,
    currency_code = CASE
        WHEN REPLACE(phone, ' ', '') LIKE '+234%' THEN 'NGN'
        WHEN REPLACE(phone, ' ', '') LIKE '+91%' THEN 'INR'
        WHEN REPLACE(phone, ' ', '') LIKE '+237%' THEN 'XAF'
        WHEN REPLACE(phone, ' ', '') LIKE '+233%' THEN 'GHS'
        WHEN REPLACE(phone, ' ', '') LIKE '+254%' THEN 'KES'
        WHEN REPLACE(phone, ' ', '') LIKE '+1%' THEN 'USD'
        ELSE NULL
    END,
    country_binding_status = CASE
        WHEN role_code <> 'USER' THEN NULL
        WHEN phone IS NOT NULL AND (
            REPLACE(phone, ' ', '') LIKE '+234%'
            OR REPLACE(phone, ' ', '') LIKE '+91%'
            OR REPLACE(phone, ' ', '') LIKE '+237%'
            OR REPLACE(phone, ' ', '') LIKE '+233%'
            OR REPLACE(phone, ' ', '') LIKE '+254%'
            OR REPLACE(phone, ' ', '') LIKE '+1%'
        ) THEN 'BOUND'
        ELSE 'UNRESOLVED'
    END,
    country_bound_at = CASE
        WHEN role_code = 'USER' AND phone IS NOT NULL AND (
            REPLACE(phone, ' ', '') LIKE '+234%'
            OR REPLACE(phone, ' ', '') LIKE '+91%'
            OR REPLACE(phone, ' ', '') LIKE '+237%'
            OR REPLACE(phone, ' ', '') LIKE '+233%'
            OR REPLACE(phone, ' ', '') LIKE '+254%'
            OR REPLACE(phone, ' ', '') LIKE '+1%'
        ) THEN CURRENT_TIMESTAMP
        ELSE NULL
    END;
