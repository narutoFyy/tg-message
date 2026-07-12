ALTER TABLE lottery_prize ADD COLUMN base_amount_usd DECIMAL(18, 6);

UPDATE lottery_prize SET base_amount_usd = 0.066667 WHERE id = 'lottery-prize-ngn-1000';
UPDATE lottery_prize SET base_amount_usd = 0.133333 WHERE id = 'lottery-prize-ngn-2000';
UPDATE lottery_prize SET base_amount_usd = 0.333333 WHERE id = 'lottery-prize-ngn-3000';
UPDATE lottery_prize SET base_amount_usd = 0.666667 WHERE id = 'lottery-prize-ngn-5000';
UPDATE lottery_prize SET base_amount_usd = 1.333333 WHERE id = 'lottery-prize-ngn-8000';
UPDATE lottery_prize SET base_amount_usd = 2.000000 WHERE id = 'lottery-prize-ngn-10000';
UPDATE lottery_prize SET base_amount_usd = 3.333333 WHERE id = 'lottery-prize-ngn-15000';

ALTER TABLE lottery_draw_record ADD COLUMN base_amount_usd DECIMAL(18, 6);
ALTER TABLE lottery_draw_record ADD COLUMN local_amount DECIMAL(18, 2);
ALTER TABLE lottery_draw_record ADD COLUMN currency_code VARCHAR(3);
ALTER TABLE lottery_draw_record ADD COLUMN exchange_rate_snapshot DECIMAL(18, 6);

ALTER TABLE trade_order ADD COLUMN base_amount_usd DECIMAL(18, 6);
ALTER TABLE trade_order ADD COLUMN local_amount DECIMAL(18, 2);
ALTER TABLE trade_order ADD COLUMN currency_code VARCHAR(3);
ALTER TABLE trade_order ADD COLUMN business_rate_snapshot DECIMAL(18, 6);
