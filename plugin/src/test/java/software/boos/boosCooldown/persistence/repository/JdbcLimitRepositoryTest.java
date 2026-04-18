package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.model.LimitEntry;
import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcLimitRepositoryTest {

    private LimitRepository repo;
    private UUID player;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        repo = new JdbcLimitRepository(ds, StorageBackend.SQLITE);
        player = UUID.randomUUID();
    }

    @Test
    void upsertPersistsWithoutResetAt() {
        repo.upsert(new LimitEntry(player, "/kit", 3, null));
        LimitEntry loaded = repo.find(player, "/kit").orElseThrow();
        assertEquals(3, loaded.remainingUses());
        assertNull(loaded.resetAt());
    }

    @Test
    void upsertPersistsWithResetAt() {
        Instant reset = Instant.now().plusSeconds(60);
        repo.upsert(new LimitEntry(player, "/kit", 2, reset));
        LimitEntry loaded = repo.find(player, "/kit").orElseThrow();
        assertEquals(reset.toEpochMilli(), loaded.resetAt().toEpochMilli());
    }

    @Test
    void upsertOverwritesUses() {
        repo.upsert(new LimitEntry(player, "/kit", 5, null));
        repo.upsert(new LimitEntry(player, "/kit", 4, null));
        assertEquals(4, repo.find(player, "/kit").orElseThrow().remainingUses());
    }

    @Test
    void deleteAllForPlayerWipesPlayer() {
        repo.upsert(new LimitEntry(player, "/kit", 1, null));
        repo.upsert(new LimitEntry(player, "/home", 1, null));
        repo.deleteAllForPlayer(player);
        assertTrue(repo.find(player, "/kit").isEmpty());
        assertTrue(repo.find(player, "/home").isEmpty());
    }

    @Test
    void deleteAllForCommandWipesAcrossPlayers() {
        UUID other = UUID.randomUUID();
        repo.upsert(new LimitEntry(player, "/kit", 1, null));
        repo.upsert(new LimitEntry(other, "/kit", 1, null));
        repo.upsert(new LimitEntry(player, "/home", 1, null));
        int removed = repo.deleteAllForCommand("/kit");
        assertEquals(2, removed);
        assertTrue(repo.find(player, "/kit").isEmpty());
        assertTrue(repo.find(other, "/kit").isEmpty());
        assertTrue(repo.find(player, "/home").isPresent());
    }
}
