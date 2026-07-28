UPDATE lottery_prize
SET enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    'lottery-prize-ipad',
    'lottery-prize-iphone-17',
    'lottery-prize-computer'
);
