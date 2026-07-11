ALTER TABLE gift_card_rate
ADD COLUMN card_code VARCHAR(64);

UPDATE gift_card_rate
SET card_code = 'APPLE_ITUNES',
    card_name = 'Apple / iTunes'
WHERE LOWER(TRIM(card_name)) IN ('apple', 'itunes', 'apple(itunes)', 'apple itunes', 'apple / itunes');

UPDATE gift_card_rate
SET card_code = 'STEAM',
    card_name = 'Steam'
WHERE LOWER(TRIM(card_name)) IN ('steam', 'steam card', 'steam gift card');

UPDATE gift_card_rate
SET card_code = 'RAZER_GOLD',
    card_name = 'Razer Gold'
WHERE LOWER(TRIM(card_name)) IN ('razer', 'razergold', 'razer gold');

UPDATE gift_card_rate
SET card_code = 'XBOX',
    card_name = 'Xbox'
WHERE LOWER(TRIM(card_name)) IN ('xbox', 'xbox card', 'xbox gift card');

UPDATE gift_card_rate
SET card_code = 'EBAY',
    card_name = 'eBay'
WHERE LOWER(TRIM(card_name)) IN ('ebay', 'ebay card');

UPDATE gift_card_rate
SET card_code = 'SEPHORA',
    card_name = 'Sephora'
WHERE LOWER(TRIM(card_name)) IN ('sephora', 'sephora card');

UPDATE gift_card_rate
SET card_code = 'GOOGLE_PLAY',
    card_name = 'Google Play'
WHERE LOWER(TRIM(card_name)) IN ('google', 'google card', 'google play', 'google play card');

UPDATE gift_card_rate
SET card_code = 'VANILLA',
    card_name = 'Vanilla'
WHERE LOWER(TRIM(card_name)) IN ('vanilla', 'vanilla card', 'vanilla gift card');

UPDATE gift_card_rate
SET card_code = 'AMERICAN_EXPRESS',
    card_name = 'American Express'
WHERE LOWER(TRIM(card_name)) IN ('amex', 'american express', 'american express card');

UPDATE gift_card_rate
SET card_code = 'ZELLE',
    card_name = 'Zelle'
WHERE LOWER(TRIM(card_name)) IN ('zelle', 'zelle card');

UPDATE gift_card_rate
SET card_code = 'CHIME',
    card_name = 'Chime'
WHERE LOWER(TRIM(card_name)) IN ('chime', 'chime card');
