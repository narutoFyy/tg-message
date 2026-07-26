UPDATE lottery_prize
SET name = '₦800',
    base_amount_usd = 0.533333,
    sort_order = 30,
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-1000';

UPDATE lottery_prize
SET sort_order = 10,
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-2000';

UPDATE lottery_prize
SET sort_order = 20,
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-3000';

UPDATE lottery_prize
SET sort_order = 40,
    enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-5000';

UPDATE lottery_prize
SET enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    'lottery-prize-ngn-8000',
    'lottery-prize-ngn-10000',
    'lottery-prize-ngn-15000',
    'lottery-prize-ipad',
    'lottery-prize-iphone-17',
    'lottery-prize-computer'
);
