ALTER TABLE user_vaults ADD COLUMN expires_at TEXT;

UPDATE user_vaults
SET expires_at = datetime(updated_at, '+180 days')
WHERE expires_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_vaults_expires_at
ON user_vaults(expires_at);
