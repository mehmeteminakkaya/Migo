CREATE TABLE IF NOT EXISTS user_vaults (
    user_id TEXT PRIMARY KEY NOT NULL,
    auth_hash TEXT NOT NULL,
    session_json TEXT NOT NULL,
    session_version INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CHECK (length(user_id) BETWEEN 16 AND 128),
    CHECK (length(auth_hash) = 64),
    CHECK (length(session_json) <= 250000)
);

CREATE INDEX IF NOT EXISTS idx_user_vaults_updated_at
    ON user_vaults(updated_at);
