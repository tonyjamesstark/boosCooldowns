package software.boos.boosCooldown.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import software.boos.boosCooldown.config.DatabaseConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class DataSourceFactory {

    private DataSourceFactory() {
    }

    public static DataSource create(DatabaseConfig config) {
        loadDriver(config.backend());

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("BoosCooldowns-Pool");
        hikari.setJdbcUrl(config.jdbcUrl());
        if (!config.username().isEmpty()) {
            hikari.setUsername(config.username());
            hikari.setPassword(config.password());
        }
        hikari.setMaximumPoolSize(Math.max(1, config.poolSize()));
        hikari.setConnectionTimeout(config.connectionTimeoutMs());
        hikari.setMinimumIdle(1);
        hikari.setIdleTimeout(300_000L);
        hikari.setMaxLifetime(1_800_000L);
        // Warns if a connection is held for >10s without being closed
        // (indicates a leaking try-with-resources or a blocked thread).
        hikari.setLeakDetectionThreshold(10_000L);

        if (config.backend() == StorageBackend.SQLITE) {
            // SQLite has limited concurrent write support; single writer is safest.
            hikari.setMaximumPoolSize(1);
            hikari.addDataSourceProperty("journal_mode", "WAL");
            hikari.addDataSourceProperty("synchronous", "NORMAL");
            hikari.addDataSourceProperty("foreign_keys", "ON");
        } else {
            hikari.addDataSourceProperty("cachePrepStmts", "true");
            hikari.addDataSourceProperty("prepStmtCacheSize", "250");
            hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikari.addDataSourceProperty("useServerPrepStmts", "true");
        }

        HikariDataSource dataSource = new HikariDataSource(hikari);
        verify(dataSource);
        return dataSource;
    }

    private static void loadDriver(StorageBackend backend) {
        try {
            switch (backend) {
                case SQLITE -> Class.forName("org.sqlite.JDBC");
                case MYSQL -> Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver for " + backend + " not found on classpath. "
                    + "Spigot should auto-download it via plugin.yml 'libraries'.", e);
        }
    }

    private static void verify(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(5)) {
                throw new SQLException("Connection validation failed");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to establish initial database connection", e);
        }
    }
}
