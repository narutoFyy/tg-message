UPDATE support_message
SET content = REPLACE(content, 'CardBrother', 'Xcard')
WHERE sender_role = 'SUPPORT'
  AND content LIKE '%CardBrother%';

UPDATE agent_welcome_message
SET content = REPLACE(content, 'CardBrother', 'Xcard'),
    updated_at = CURRENT_TIMESTAMP
WHERE content LIKE '%CardBrother%';
