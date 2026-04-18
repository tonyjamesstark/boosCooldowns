CREATE TABLE IF NOT EXISTS user_preferences (
    player_uuid CHAR(36) NOT NULL,
    pref_key VARCHAR(64) NOT NULL,
    pref_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (player_uuid, pref_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
