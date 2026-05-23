ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS jellyfin_user_id VARCHAR(255);

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(32) NOT NULL DEFAULT 'FILM';

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS release_year INT;

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS genres TEXT NOT NULL DEFAULT '';

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS cast_members TEXT NOT NULL DEFAULT '';

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS directors TEXT NOT NULL DEFAULT '';

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS imdb_rating DOUBLE PRECISION;

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS platform_rating DOUBLE PRECISION;

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS external_url TEXT;

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS jellyfin_item_id VARCHAR(255);

ALTER TABLE public.films
    ADD COLUMN IF NOT EXISTS jellyfin_library_id VARCHAR(255);

ALTER TABLE public.favorites
    ADD COLUMN IF NOT EXISTS watched_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS public.user_preferences (
    user_id UUID PRIMARY KEY,
    weighted_genres TEXT NOT NULL DEFAULT '',
    plot_types TEXT NOT NULL DEFAULT '',
    eras TEXT NOT NULL DEFAULT '',
    cast_and_directors TEXT NOT NULL DEFAULT '',
    moods TEXT NOT NULL DEFAULT '',
    content_types TEXT NOT NULL DEFAULT '',
    CONSTRAINT user_preferences_user_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.film_ratings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    film_id UUID NOT NULL,
    score INT NOT NULL,
    note VARCHAR(2048),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT film_ratings_user_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT film_ratings_film_fk FOREIGN KEY (film_id) REFERENCES public.films(id) ON DELETE CASCADE,
    CONSTRAINT film_ratings_score_range CHECK (score >= 1 AND score <= 10),
    CONSTRAINT film_ratings_user_film_unique UNIQUE (user_id, film_id)
);

CREATE TABLE IF NOT EXISTS public.jellyfin_sync_state (
    user_id UUID PRIMARY KEY,
    last_synced_at TIMESTAMP,
    last_successful_sync_at TIMESTAMP,
    last_error TEXT,
    synced_item_count INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT jellyfin_sync_state_user_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);
