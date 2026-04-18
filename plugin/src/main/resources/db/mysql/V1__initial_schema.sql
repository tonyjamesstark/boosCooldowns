CREATE TABLE IF NOT EXISTS schema_version (
    version INT PRIMARY KEY,
    applied_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cooldowns (
    player_uuid CHAR(36) NOT NULL,
    command_key VARCHAR(191) NOT NULL,
    expires_at BIGINT NOT NULL,
    PRIMARY KEY (player_uuid, command_key),
    INDEX idx_cooldowns_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS limit_uses (
    player_uuid CHAR(36) NOT NULL,
    command_key VARCHAR(191) NOT NULL,
    remaining_uses INT NOT NULL,
    reset_at BIGINT NULL,
    PRIMARY KEY (player_uuid, command_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS global_limits (
    command_key VARCHAR(191) NOT NULL PRIMARY KEY,
    reset_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS server_cooldowns (
    command_key VARCHAR(191) NOT NULL PRIMARY KEY,
    expires_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
