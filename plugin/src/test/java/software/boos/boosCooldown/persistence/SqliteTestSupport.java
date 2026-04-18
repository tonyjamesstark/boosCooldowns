package software.boos.boosCooldown.persistence;

import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Helpers for spinning up an isolated SQLite DB per test. Uses a file-based
 * DB in a temp directory (rather than :memory:) so that multiple connections
 * share the same schema via HikariCP-like semantics without the pragmas.
 */
public final class SqliteTestSupport {

    private SqliteTestSupport() {
    }

    public static DataSource createMigratedDataSource(Path tempDir) throws IOException {
        Files.createDirectories(tempDir);
        Path dbFile = tempDir.resolve("test-" + System.nanoTime() + ".db");
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + dbFile.toAbsolutePath());
        new SchemaMigrator(ds, StorageBackend.SQLITE, Logger.getLogger("SqliteTestSupport")).migrate();
        return ds;
    }
}
