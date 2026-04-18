package software.boos.boosCooldown.config;

import software.boos.boosCooldown.persistence.StorageBackend;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.Objects;

public final class DatabaseConfig {

    private final StorageBackend backend;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int poolSize;
    private final long connectionTimeoutMs;
    private final long cacheTtlMs;

    private DatabaseConfig(StorageBackend backend, String jdbcUrl, String username,
                           String password, int poolSize, long connectionTimeoutMs, long cacheTtlMs) {
        this.backend = backend;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.cacheTtlMs = cacheTtlMs;
    }

    public static DatabaseConfig fromSection(ConfigurationSection section, File pluginDataFolder) {
        Objects.requireNonNull(pluginDataFolder, "pluginDataFolder");
        String typeRaw = section != null ? section.getString("type", "sqlite") : "sqlite";
        StorageBackend backend = StorageBackend.parse(typeRaw);
        String username = section != null ? section.getString("username", "") : "";
        String password = section != null ? section.getString("password", "") : "";
        int poolSize = section != null ? section.getInt("pool-size", 4) : 4;
        long timeout = section != null ? section.getLong("connection-timeout-ms", 10_000L) : 10_000L;
        long cacheTtl = section != null ? section.getLong("cache-ttl-seconds", 30L) * 1000L : 30_000L;

        String jdbcUrl = resolveJdbcUrl(backend, section, pluginDataFolder);
        return new DatabaseConfig(backend, jdbcUrl, username, password, poolSize, timeout, cacheTtl);
    }

    private static String resolveJdbcUrl(StorageBackend backend, ConfigurationSection section, File dataFolder) {
        if (section != null) {
            String explicit = section.getString("jdbc-url");
            if (explicit != null && !explicit.isBlank()) {
                return explicit;
            }
        }
        return switch (backend) {
            case SQLITE -> "jdbc:sqlite:" + new File(dataFolder, "cooldowns.db").getAbsolutePath();
            case MYSQL -> {
                String host = section != null ? section.getString("host", "localhost") : "localhost";
                int port = section != null ? section.getInt("port", 3306) : 3306;
                String database = section != null ? section.getString("database", "boosCooldowns") : "boosCooldowns";
                yield "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=false&useUnicode=true&characterEncoding=UTF-8"
                        + "&serverTimezone=UTC&autoReconnect=true";
            }
        };
    }

    public StorageBackend backend() {
        return backend;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public int poolSize() {
        return poolSize;
    }

    public long connectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public long cacheTtlMs() {
        return cacheTtlMs;
    }
}
