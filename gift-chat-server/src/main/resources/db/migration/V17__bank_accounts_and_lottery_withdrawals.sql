CREATE TABLE user_bank_account (
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

ALTER TABLE withdrawal_request
    ADD COLUMN bank_account_id VARCHAR(36);

ALTER TABLE withdrawal_request
    ADD COLUMN lottery_draw_record_id VARCHAR(36);

ALTER TABLE withdrawal_request
    ADD CONSTRAINT fk_withdrawal_bank_account FOREIGN KEY (bank_account_id) REFERENCES user_bank_account (id);

ALTER TABLE withdrawal_request
    ADD CONSTRAINT fk_withdrawal_lottery_draw FOREIGN KEY (lottery_draw_record_id) REFERENCES lottery_draw_record (id);

CREATE UNIQUE INDEX ux_withdrawal_lottery_draw
    ON withdrawal_request (lottery_draw_record_id);
