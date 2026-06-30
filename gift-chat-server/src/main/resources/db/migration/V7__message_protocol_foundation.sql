ALTER TABLE support_message ADD COLUMN IF NOT EXISTS client_message_id VARCHAR(64);
ALTER TABLE support_message ADD COLUMN IF NOT EXISTS server_seq BIGINT NOT NULL DEFAULT 0;
ALTER TABLE support_message ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED';
ALTER TABLE support_message ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;
ALTER TABLE support_message ADD COLUMN IF NOT EXISTS failed_reason VARCHAR(255);

ALTER TABLE direct_message ADD COLUMN IF NOT EXISTS client_message_id VARCHAR(64);
ALTER TABLE direct_message ADD COLUMN IF NOT EXISTS server_seq BIGINT NOT NULL DEFAULT 0;
ALTER TABLE direct_message ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(32) NOT NULL DEFAULT 'DELIVERED';
ALTER TABLE direct_message ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;
ALTER TABLE direct_message ADD COLUMN IF NOT EXISTS failed_reason VARCHAR(255);

UPDATE support_message sm
SET server_seq = (
    SELECT COUNT(*)
    FROM support_message older
    WHERE older.conversation_id = sm.conversation_id
      AND (older.created_at < sm.created_at OR (older.created_at = sm.created_at AND older.id <= sm.id))
)
WHERE sm.server_seq IS NULL;

UPDATE direct_message dm
SET server_seq = (
    SELECT COUNT(*)
    FROM direct_message older
    WHERE older.friendship_id = dm.friendship_id
      AND (older.created_at < dm.created_at OR (older.created_at = dm.created_at AND older.id <= dm.id))
)
WHERE dm.server_seq IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_support_message_client_id ON support_message (sender_user_id, client_message_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_direct_message_client_id ON direct_message (sender_user_id, client_message_id);
