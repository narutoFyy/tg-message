CREATE TABLE IF NOT EXISTS video_session (
    id VARCHAR(36) PRIMARY KEY,
    room_id VARCHAR(64) NOT NULL UNIQUE,
    channel_type VARCHAR(32) NOT NULL,
    channel_id VARCHAR(64) NOT NULL,
    initiator_user_id VARCHAR(36) NOT NULL,
    receiver_user_id VARCHAR(36) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    vendor_code VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_video_initiator FOREIGN KEY (initiator_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_video_receiver FOREIGN KEY (receiver_user_id) REFERENCES app_user (id)
);
