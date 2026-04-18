CREATE TABLE IF NOT EXISTS user_preferences (
    player_uuid TEXT NOT NULL,
    pref_key TEXT NOT NULL,
    pref_value TEXT NOT NULL,
    PRIMARY KEY (player_uuid, pref_key)
);
