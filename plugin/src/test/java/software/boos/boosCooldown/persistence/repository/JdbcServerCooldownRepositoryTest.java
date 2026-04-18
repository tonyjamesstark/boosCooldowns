package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.model.ServerCooldownEntry;
import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcServerCooldownRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcServerCooldownRepositoryTest {

    private ServerCooldownRepository repo;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        repo = new JdbcServerCooldownRepository(ds, StorageBackend.SQLITE);
    }

    @Test
    void findReturnsEmptyWhenMissing() {
        assertTrue(repo.find("/event").isEmpty());
    }

    @Test
    void upsertPersistsAndLoads() {
        Instant expires = Instant.ofEpochMilli(123_456_789L);
        repo.upsert(new ServerCooldownEntry("/event", expires));
        assertEquals(expires, repo.find("/event").orElseThrow().expiresAt());
    }

    @Test
    void upsertOverwritesExpiry() {
        repo.upsert(new ServerCooldownEntry("/event", Instant.ofEpochMilli(1)));
        repo.upsert(new ServerCooldownEntry("/event", Instant.ofEpochMilli(2)));
        assertEquals(2L, repo.find("/event").orElseThrow().expiresAt().toEpochMilli());
    }

    @Test
    void deleteRemovesEntry() {
        repo.upsert(new ServerCooldownEntry("/event", Instant.now()));
        repo.delete("/event");
        assertTrue(repo.find("/event").isEmpty());
    }
}
