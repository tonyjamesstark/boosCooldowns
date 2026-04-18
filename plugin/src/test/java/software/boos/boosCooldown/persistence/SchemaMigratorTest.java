package software.boos.boosCooldown.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaMigratorTest {

    @Test
    void migrateCreatesAllTables(@TempDir Path tempDir) throws Exception {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        try (Connection c = ds.getConnection()) {
            assertTableExists(c, "cooldowns");
            assertTableExists(c, "limit_uses");
            assertTableExists(c, "global_limits");
            assertTableExists(c, "server_cooldowns");
            assertTableExists(c, "user_preferences");
            assertTableExists(c, "schema_version");
        }
    }

    @Test
    void migrateIsIdempotent(@TempDir Path tempDir) throws IOException {
        Path dbFile = tempDir.resolve("repeat.db");
        org.sqlite.SQLiteDataSource ds = new org.sqlite.SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());

        SchemaMigrator migrator = new SchemaMigrator(ds, StorageBackend.SQLITE,
                java.util.logging.Logger.getLogger("T"));
        migrator.migrate();
        migrator.migrate(); // second call should be a no-op

        try (Connection c = ds.getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM schema_version")) {
            rs.next();
            // Every migration added to SchemaMigrator.MIGRATIONS is reflected here.
            assertEquals(4, rs.getInt(1));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void assertTableExists(Connection c, String tableName) throws Exception {
        try (Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'")) {
            assertTrue(rs.next(), "Expected table " + tableName + " to exist");
        }
    }
}
