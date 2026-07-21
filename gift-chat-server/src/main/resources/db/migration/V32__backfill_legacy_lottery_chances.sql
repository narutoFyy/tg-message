INSERT INTO lottery_chance_ledger (
    id,
    user_id,
    source_key,
    source_type,
    vip_level,
    period_type,
    period_key,
    granted_at,
    consumed_at
)
SELECT
    u.id,
    u.id,
    CONCAT('WELCOME:', u.id),
    'WELCOME',
    'VIP0',
    'ONCE',
    'WELCOME',
    MIN(r.drawn_at),
    MIN(r.drawn_at)
FROM app_user u
JOIN lottery_draw_record r ON r.user_id = u.id
WHERE u.role_code = 'USER'
  AND NOT EXISTS (
      SELECT 1
      FROM lottery_chance_ledger c
      WHERE c.source_key = CONCAT('WELCOME:', u.id)
  )
GROUP BY u.id;
