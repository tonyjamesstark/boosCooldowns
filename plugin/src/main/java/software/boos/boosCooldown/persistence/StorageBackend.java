package software.boos.boosCooldown.persistence;

import java.util.Locale;

public enum StorageBackend {
    SQLITE,
    MYSQL;

    public static StorageBackend parse(String raw) {
        if (raw == null) {
            return SQLITE;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MYSQL", "MARIADB" -> MYSQL;
            case "SQLITE" -> SQLITE;
            default -> throw new IllegalArgumentException(
                    "Unknown storage backend: " + raw + " (allowed: sqlite, mysql, mariadb)");
        };
    }
}
