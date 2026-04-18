package software.boos.boosCooldown.persistence;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class SchemaMigrator {

    private static final TreeMap<Integer, String> MIGRATIONS = new TreeMap<>();

    static {
        MIGRATIONS.put(1, "V1__initial_schema.sql");
        MIGRATIONS.put(2, "V2__user_preferences.sql");
        MIGRATIONS.put(3, "V3__audit_log.sql");
        MIGRATIONS.put(4, "V4__config_version.sql");
    }

    private final DataSource dataSource;
    private final StorageBackend backend;
    private final Logger logger;

    public SchemaMigrator(DataSource dataSource, StorageBackend backend, Logger logger) {
        this.dataSource = dataSource;
        this.backend = backend;
        this.logger = logger;
    }

    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            ensureVersionTable(connection);
            int current = currentVersion(connection);
            for (var entry : MIGRATIONS.tailMap(current, false).entrySet()) {
                int version = entry.getKey();
                String resource = dialectFolder() + "/" + entry.getValue();
                logger.info("[boosCooldowns] Applying DB migration " + resource);
                applyMigration(connection, version, resource);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Schema migration failed", e);
        }
    }

    private String dialectFolder() {
        return switch (backend) {
            case SQLITE -> "db/sqlite";
            case MYSQL -> "db/mysql";
        };
    }

    private void ensureVersionTable(Connection connection) throws SQLException {
        String ddl = switch (backend) {
            case SQLITE -> "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, applied_at INTEGER NOT NULL)";
            case MYSQL -> "CREATE TABLE IF NOT EXISTS schema_version (version INT PRIMARY KEY, applied_at BIGINT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        };
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(ddl);
        }
    }

    private int currentVersion(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(MAX(version), 0) FROM schema_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void applyMigration(Connection connection, int version, String resource) throws SQLException {
        String sql = readResource(resource);
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            for (String statement : splitStatements(sql)) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute(statement);
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO schema_version (version, applied_at) VALUES (?, ?)")) {
                ps.setInt(1, version);
                ps.setLong(2, System.currentTimeMillis());
                ps.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private String readResource(String path) {
        try (InputStream in = SchemaMigrator.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Migration resource not found: " + path);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read migration: " + path, e);
        }
    }

    private List<String> splitStatements(String sql) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                String statement = current.toString().trim();
                statement = statement.substring(0, statement.length() - 1);
                result.add(statement);
                current.setLength(0);
            }
        }
        if (!current.toString().trim().isEmpty()) {
            result.add(current.toString().trim());
        }
        return result;
    }
}
