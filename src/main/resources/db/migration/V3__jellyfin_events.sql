-- Create table to store Jellyfin events for idempotency and auditing
CREATE TABLE IF NOT EXISTS jellyfin_events (
    event_id VARCHAR(255) PRIMARY KEY,
    server_id VARCHAR(255),
    event_type VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE,
    jellyfin_user_id VARCHAR(255),
    jellyfin_item_id VARCHAR(255),
    payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_jellyfin_events_user ON jellyfin_events(jellyfin_user_id);
CREATE INDEX IF NOT EXISTS idx_jellyfin_events_item ON jellyfin_events(jellyfin_item_id);
