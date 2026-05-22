DROP TABLE IF EXISTS public.ratings;

ALTER TABLE public.users
    DROP COLUMN IF EXISTS jellyfin_id;

ALTER TABLE public.films
    DROP COLUMN IF EXISTS jellyfin_id;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_jellyfin_user_id ON public.users(jellyfin_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_films_jellyfin_item_id ON public.films(jellyfin_item_id);
CREATE INDEX IF NOT EXISTS idx_films_jellyfin_library_id ON public.films(jellyfin_library_id);
