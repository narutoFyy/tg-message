CREATE TABLE invite_code_registry (
    code VARCHAR(32) PRIMARY KEY,
    code_type VARCHAR(16) NOT NULL,
    owner_user_id VARCHAR(36),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_invite_code_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_invite_code_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE UNIQUE INDEX ux_invite_code_owner
    ON invite_code_registry (owner_user_id);

CREATE INDEX idx_invite_code_type_created
    ON invite_code_registry (code_type, created_at);

INSERT INTO invite_code_registry (
    code,
    code_type,
    owner_user_id,
    enabled,
    created_by_user_id,
    created_at,
    updated_at
)
SELECT
    UPPER(invite_code),
    'PERSONAL',
    id,
    TRUE,
    NULL,
    created_at,
    updated_at
FROM app_user
WHERE invite_code IS NOT NULL AND TRIM(invite_code) <> '';

ALTER TABLE app_user
    ADD COLUMN registration_invite_code VARCHAR(32);

UPDATE app_user
SET registration_invite_code = (
    SELECT invite_code_registry.code
    FROM invite_code_registry
    WHERE invite_code_registry.owner_user_id = app_user.referred_by_user_id
)
WHERE referred_by_user_id IS NOT NULL;

ALTER TABLE app_user
    ADD CONSTRAINT fk_app_user_registration_invite_code
        FOREIGN KEY (registration_invite_code) REFERENCES invite_code_registry (code);

CREATE INDEX idx_app_user_registration_invite_code
    ON app_user (registration_invite_code);
