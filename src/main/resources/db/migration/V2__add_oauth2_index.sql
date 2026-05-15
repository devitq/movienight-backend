CREATE UNIQUE INDEX IF NOT EXISTS idx_users_provider_provider_id
ON users(provider, provider_id);
