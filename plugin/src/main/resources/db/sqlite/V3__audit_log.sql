CREATE TABLE IF NOT EXISTS command_audit (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    at_millis INTEGER NOT NULL,
    player_uuid TEXT NOT NULL,
    player_name TEXT NOT NULL,
    command TEXT NOT NULL,
    outcome TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_at ON command_audit (at_millis);
CREATE INDEX IF NOT EXISTS idx_audit_player ON command_audit (player_uuid);
