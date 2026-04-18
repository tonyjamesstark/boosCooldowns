CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    applied_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS cooldowns (
    player_uuid TEXT NOT NULL,
    command_key TEXT NOT NULL,
    expires_at INTEGER NOT NULL,
    PRIMARY KEY (player_uuid, command_key)
);

CREATE INDEX IF NOT EXISTS idx_cooldowns_expires ON cooldowns (expires_at);

CREATE TABLE IF NOT EXISTS limit_uses (
    player_uuid TEXT NOT NULL,
    command_key TEXT NOT NULL,
    remaining_uses INTEGER NOT NULL,
    reset_at INTEGER,
    PRIMARY KEY (player_uuid, command_key)
);

CREATE TABLE IF NOT EXISTS global_limits (
    command_key TEXT NOT NULL PRIMARY KEY,
    reset_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS server_cooldowns (
    command_key TEXT NOT NULL PRIMARY KEY,
    expires_at INTEGER NOT NULL
);
