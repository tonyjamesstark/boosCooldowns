package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcConfigVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcConfigVersionRepositoryTest {

    private ConfigVersionRepository repo;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        repo = new JdbcConfigVersionRepository(ds, StorageBackend.SQLITE);
    }

    @Test
    void upsertInsertsNewNode() {
        Instant now = Instant.ofEpochMilli(1_000_000L);
        repo.upsert("node-a", "hash-abc", now);
        var entry = repo.find("node-a").orElseThrow();
        assertEquals("hash-abc", entry.contentHash());
        assertEquals(now, entry.reloadedAt());
    }

    @Test
    void upsertReplacesExisting() {
        repo.upsert("node-a", "hash-v1", Instant.ofEpochMilli(1));
        repo.upsert("node-a", "hash-v2", Instant.ofEpochMilli(2));
        var entry = repo.find("node-a").orElseThrow();
        assertEquals("hash-v2", entry.contentHash());
        assertEquals(Instant.ofEpochMilli(2), entry.reloadedAt());
    }

    @Test
    void findAllOrdersByReloadedAtDesc() {
        repo.upsert("node-a", "hash-a", Instant.ofEpochMilli(100));
        repo.upsert("node-b", "hash-b", Instant.ofEpochMilli(300));
        repo.upsert("node-c", "hash-c", Instant.ofEpochMilli(200));
        List<?> all = repo.findAll();
        assertEquals(3, all.size());
        // The most recently reloaded node comes first
        assertEquals("node-b", ((software.boos.boosCooldown.model.ConfigVersion) all.get(0)).nodeId());
    }

    @Test
    void deleteRemovesNode() {
        repo.upsert("node-a", "hash", Instant.now());
        repo.delete("node-a");
        assertTrue(repo.find("node-a").isEmpty());
    }
}
