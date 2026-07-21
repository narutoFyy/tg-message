ALTER TABLE trade_order ADD COLUMN face_currency_code VARCHAR(3);
ALTER TABLE trade_order ADD COLUMN face_value_amount DECIMAL(18, 6);
ALTER TABLE trade_order ADD COLUMN quantity_value INT;
ALTER TABLE trade_order ADD COLUMN face_to_usd_rate_snapshot DECIMAL(18, 6);
