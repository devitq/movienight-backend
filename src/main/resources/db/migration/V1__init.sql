CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    provider VARCHAR(64),
    provider_id VARCHAR(255),
    jellyfin_user_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.films (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    content_type VARCHAR(32) NOT NULL DEFAULT 'FILM',
    release_year INT,
    genres TEXT NOT NULL DEFAULT '',
    cast_members TEXT NOT NULL DEFAULT '',
    directors TEXT NOT NULL DEFAULT '',
    imdb_rating DOUBLE PRECISION,
    platform_rating DOUBLE PRECISION,
    external_url TEXT,
    jellyfin_item_id VARCHAR(255),
    jellyfin_library_id VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS public.favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    film_id UUID NOT NULL,
    comment VARCHAR(1024),
    is_viewed BOOLEAN NOT NULL DEFAULT FALSE,
    watched_at TIMESTAMP,
    CONSTRAINT favorites_user_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT favorites_film_fk FOREIGN KEY (film_id) REFERENCES public.films(id) ON DELETE CASCADE
);

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
