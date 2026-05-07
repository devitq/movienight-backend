CREATE UNIQUE INDEX IF NOT EXISTS idx_users_provider_provider_id
ON users(provider, provider_id)
WHERE provider IS NOT NULL AND provider_id IS NOT NULL;
