-- Add jellyfin_id columns for mapping between MovieNight and Jellyfin
ALTER TABLE films
    ADD COLUMN jellyfin_id UUID UNIQUE NULL,
    ADD COLUMN jellyfin_library_id UUID NULL;

CREATE INDEX idx_films_jellyfin_id ON films(jellyfin_id);

-- Add jellyfin_id to users for sync mapping
ALTER TABLE users
    ADD COLUMN jellyfin_id UUID UNIQUE NULL;

CREATE INDEX idx_users_jellyfin_id ON users(jellyfin_id);
