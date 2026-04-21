ALTER TABLE public.users ADD COLUMN provider VARCHAR(20);
ALTER TABLE public.users ADD COLUMN provider_id VARCHAR(255);
ALTER TABLE public.users ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;

CREATE UNIQUE INDEX idx_users_provider_provider_id
    ON public.users(provider, provider_id);

CREATE INDEX idx_users_email ON public.users(email);

