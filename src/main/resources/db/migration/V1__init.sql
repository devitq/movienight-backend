DROP TABLE IF EXISTS public.favorites;
DROP TABLE IF EXISTS public.films;
DROP TABLE IF EXISTS public.users;

CREATE TABLE public.users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE
);

CREATE TABLE public.films (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE public.favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    film_id UUID NOT NULL,
    comment VARCHAR(1024),
    is_viewed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT favorites_user_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT favorites_film_fk FOREIGN KEY (film_id) REFERENCES public.films(id) ON DELETE CASCADE
);
