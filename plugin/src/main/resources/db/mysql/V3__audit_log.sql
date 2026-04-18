CREATE TABLE IF NOT EXISTS command_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    at_millis BIGINT NOT NULL,
    player_uuid CHAR(36) NOT NULL,
    player_name VARCHAR(32) NOT NULL,
    command VARCHAR(191) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    INDEX idx_audit_at (at_millis),
    INDEX idx_audit_player (player_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
