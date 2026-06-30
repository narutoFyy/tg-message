ALTER TABLE app_user
    ADD COLUMN tencent_user_id VARCHAR(32);

CREATE UNIQUE INDEX uq_app_user_tencent_user_id
    ON app_user (tencent_user_id);

ALTER TABLE support_message
    ADD COLUMN tencent_mirror_status VARCHAR(32) NOT NULL DEFAULT 'SKIPPED';

ALTER TABLE support_message
    ADD COLUMN tencent_message_key VARCHAR(128);

ALTER TABLE support_message
    ADD COLUMN tencent_mirrored_at TIMESTAMP;

ALTER TABLE support_message
    ADD COLUMN tencent_mirror_error VARCHAR(255);

ALTER TABLE direct_message
    ADD COLUMN tencent_mirror_status VARCHAR(32) NOT NULL DEFAULT 'SKIPPED';

ALTER TABLE direct_message
    ADD COLUMN tencent_message_key VARCHAR(128);

ALTER TABLE direct_message
    ADD COLUMN tencent_mirrored_at TIMESTAMP;

ALTER TABLE direct_message
    ADD COLUMN tencent_mirror_error VARCHAR(255);
