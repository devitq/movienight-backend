-- Add jellyfin_id columns for mapping between MovieNight and Jellyfin
ALTER TABLE films
    ADD COLUMN jellyfin_id UUID;

ALTER TABLE films
    ADD CONSTRAINT uq_films_jellyfin_id UNIQUE (jellyfin_id);

CREATE INDEX idx_films_jellyfin_id ON films(jellyfin_id);

-- Add jellyfin_id to users for sync mapping
ALTER TABLE users
    ADD COLUMN jellyfin_id UUID;

ALTER TABLE users
    ADD CONSTRAINT uq_users_jellyfin_id UNIQUE (jellyfin_id);

CREATE INDEX idx_users_jellyfin_id ON users(jellyfin_id);
