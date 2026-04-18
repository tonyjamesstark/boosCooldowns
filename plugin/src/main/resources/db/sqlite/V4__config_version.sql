CREATE TABLE IF NOT EXISTS config_version (
    node_id TEXT PRIMARY KEY,
    content_hash TEXT NOT NULL,
    reloaded_at INTEGER NOT NULL
);
