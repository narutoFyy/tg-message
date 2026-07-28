UPDATE lottery_prize
SET name = '₦200', base_amount_usd = 0.133333, sort_order = 10, enabled = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-2000';

UPDATE lottery_prize
SET name = '₦500', base_amount_usd = 0.333333, sort_order = 20, enabled = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-3000';

UPDATE lottery_prize
SET name = '₦800', base_amount_usd = 0.533333, sort_order = 30, enabled = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-1000';

UPDATE lottery_prize
SET name = '₦1000', base_amount_usd = 0.666667, sort_order = 40, enabled = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-5000';

UPDATE lottery_prize
SET name = '₦3000', base_amount_usd = 2.000000, sort_order = 50, enabled = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-10000';

UPDATE lottery_prize
SET name = '₦5000', base_amount_usd = 3.333333, sort_order = 60, enabled = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-15000';

UPDATE lottery_prize
SET name = '₦8000', base_amount_usd = 5.333333, sort_order = 70, enabled = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE id = 'lottery-prize-ngn-8000';

UPDATE lottery_prize
SET enabled = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    'lottery-prize-ipad',
    'lottery-prize-iphone-17',
    'lottery-prize-computer'
);
