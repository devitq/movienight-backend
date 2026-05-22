CREATE TABLE IF NOT EXISTS public.user_recommendation_weights (
    user_id UUID PRIMARY KEY,
    relevance_weight DOUBLE PRECISION NOT NULL DEFAULT 0.55,
    quality_weight DOUBLE PRECISION NOT NULL DEFAULT 0.15,
    context_weight DOUBLE PRECISION NOT NULL DEFAULT 0.10,
    novelty_weight DOUBLE PRECISION NOT NULL DEFAULT 0.10,
    diversity_weight DOUBLE PRECISION NOT NULL DEFAULT 0.10,
    genre_vector_weight DOUBLE PRECISION NOT NULL DEFAULT 0.25,
    plot_vector_weight DOUBLE PRECISION NOT NULL DEFAULT 0.35,
    mood_vector_weight DOUBLE PRECISION NOT NULL DEFAULT 0.15,
    era_vector_weight DOUBLE PRECISION NOT NULL DEFAULT 0.10,
    people_vector_weight DOUBLE PRECISION NOT NULL DEFAULT 0.10,
    content_type_vector_weight DOUBLE PRECISION NOT NULL DEFAULT 0.05,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_recommendation_weights_user_fk
        FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

ALTER TABLE public.recommendation_events
    ADD COLUMN IF NOT EXISTS relevance_score DOUBLE PRECISION;

ALTER TABLE public.recommendation_events
    ADD COLUMN IF NOT EXISTS quality_score DOUBLE PRECISION;

ALTER TABLE public.recommendation_events
    ADD COLUMN IF NOT EXISTS context_score DOUBLE PRECISION;

ALTER TABLE public.recommendation_events
    ADD COLUMN IF NOT EXISTS novelty_score DOUBLE PRECISION;

ALTER TABLE public.recommendation_events
    ADD COLUMN IF NOT EXISTS diversity_score DOUBLE PRECISION;

CREATE INDEX IF NOT EXISTS idx_recommendation_events_user_film_type_created
    ON public.recommendation_events(user_id, film_id, event_type, created_at DESC);
