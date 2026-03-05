-- DROP SCHEMA public;
-- CREATE SCHEMA public AUTHORIZATION pg_database_owner;
-- COMMENT ON SCHEMA public IS 'standard public schema';

-- public.genres определение
-- Drop table
DROP TABLE if exists public.genres;
CREATE TABLE public.genres (
	id int4 GENERATED ALWAYS AS IDENTITY NOT NULL,
	"name" varchar NULL,
	CONSTRAINT genres_unique PRIMARY KEY (id)
);


-- public.users определение
-- Drop table
DROP TABLE if exists public.users;
CREATE TABLE public.users (
	id int4 GENERATED ALWAYS AS IDENTITY NOT NULL,
	login varchar NOT NULL,
	"password" varchar NOT NULL,
	CONSTRAINT users_unique UNIQUE (id)
);


-- public.films определение
-- Drop table
DROP TABLE if exists public.films;
CREATE TABLE public.films (
	id int4 GENERATED ALWAYS AS IDENTITY NOT NULL,
	title varchar NOT NULL,
	genreid int4 NOT NULL,
	issuedate date NULL,
	CONSTRAINT films_unique UNIQUE(id)
);

DROP TABLE if exists public.fiml_genres;
CREATE TABLE public.film_genres (
	id int4 GENERATED ALWAYS AS IDENTITY NOT NULL,
	genreid int4 NOT NULL,
	filmid int4 NOT NULL,
	CONSTRAINT film_genres_genre_fk FOREIGN KEY (genreid) REFERENCES public.genres(id),
	CONSTRAINT film_genres_film_fk FOREIGN KEY (filmid) REFERENCES public.films(id)
);


-- Drop table
DROP TABLE if exists public.favorites;
CREATE TABLE public.favorites (
	id int4 GENERATED ALWAYS AS IDENTITY NOT NULL,
	userid int4 NULL,
	filmid int4 NULL,
	"comment" varchar NULL,
	isviewed bool NULL,
	CONSTRAINT favorites_unique PRIMARY KEY (id),
	CONSTRAINT favorites_films_fk FOREIGN KEY (filmid) REFERENCES public.films(id),
	CONSTRAINT favorites_users_fk FOREIGN KEY (userid) REFERENCES public.users(id)
);

