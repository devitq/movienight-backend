-- Create ratings table to store user film ratings
CREATE TABLE ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    film_id BIGINT NOT NULL REFERENCES films(id) ON DELETE CASCADE,
    rating NUMERIC(3, 1) NOT NULL CHECK (rating >= 0 AND rating <= 10),
    source VARCHAR(50) NOT NULL DEFAULT 'MOVIENIGHT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_film_rating UNIQUE (user_id, film_id)
);

-- Create index on user_id for efficient lookups by user
CREATE INDEX idx_ratings_user_id ON ratings(user_id);

-- Create index on film_id for efficient lookups by film
CREATE INDEX idx_ratings_film_id ON ratings(film_id);
