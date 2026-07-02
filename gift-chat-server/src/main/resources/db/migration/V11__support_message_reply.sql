ALTER TABLE support_message ADD COLUMN IF NOT EXISTS reply_to_message_id VARCHAR(36);
ALTER TABLE support_message ADD COLUMN IF NOT EXISTS reply_to_author VARCHAR(32);
ALTER TABLE support_message ADD COLUMN IF NOT EXISTS reply_to_content TEXT;

CREATE INDEX IF NOT EXISTS idx_support_message_reply_to
ON support_message (reply_to_message_id);
