CREATE TABLE lottery_chance_ledger (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    source_key VARCHAR(160) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    vip_level VARCHAR(16) NOT NULL,
    period_type VARCHAR(16) NOT NULL,
    period_key VARCHAR(32) NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    CONSTRAINT fk_lottery_chance_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT ux_lottery_chance_source UNIQUE (source_key)
);

ALTER TABLE lottery_draw_record ADD COLUMN lottery_chance_id VARCHAR(36);
ALTER TABLE lottery_draw_record ADD CONSTRAINT fk_lottery_draw_chance
    FOREIGN KEY (lottery_chance_id) REFERENCES lottery_chance_ledger (id);
ALTER TABLE lottery_draw_record ADD CONSTRAINT ux_lottery_draw_chance UNIQUE (lottery_chance_id);
