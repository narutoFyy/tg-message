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
