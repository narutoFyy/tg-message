ALTER TABLE video_session
    ADD COLUMN ringing_device_id VARCHAR(64);

ALTER TABLE video_session
    ADD COLUMN ringing_device_type VARCHAR(16);

ALTER TABLE video_session
    ADD COLUMN ringing_claimed_at TIMESTAMP;
