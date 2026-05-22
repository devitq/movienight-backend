CREATE TABLE IF NOT EXISTS public.recommendation_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    film_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    score DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT recommendation_events_user_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT recommendation_events_film_fk FOREIGN KEY (film_id) REFERENCES public.films(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_recommendation_events_user_created
    ON public.recommendation_events(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_recommendation_events_film
    ON public.recommendation_events(film_id);

CREATE INDEX IF NOT EXISTS idx_recommendation_events_type
    ON public.recommendation_events(event_type);
