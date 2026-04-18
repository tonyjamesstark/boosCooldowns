package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.model.CooldownEntry;
import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcCooldownRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcCooldownRepositoryTest {

    private DataSource dataSource;
    private CooldownRepository repo;
    private UUID playerA;
    private UUID playerB;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        dataSource = SqliteTestSupport.createMigratedDataSource(tempDir);
        repo = new JdbcCooldownRepository(dataSource, StorageBackend.SQLITE);
        playerA = UUID.randomUUID();
        playerB = UUID.randomUUID();
    }

    @Test
    void findReturnsEmptyWhenMissing() {
        assertTrue(repo.find(playerA, "/kit").isEmpty());
    }

    @Test
    void upsertThenFindReturnsEntry() {
        Instant expires = Instant.now().plusSeconds(30);
        repo.upsert(new CooldownEntry(playerA, "/kit", expires));
        Optional<CooldownEntry> found = repo.find(playerA, "/kit");
        assertTrue(found.isPresent());
        assertEquals("/kit", found.get().commandKey());
        assertEquals(expires.toEpochMilli(), found.get().expiresAt().toEpochMilli());
    }

    @Test
    void upsertOverwritesExistingExpiry() {
        repo.upsert(new CooldownEntry(playerA, "/kit", Instant.ofEpochMilli(1_000L)));
        repo.upsert(new CooldownEntry(playerA, "/kit", Instant.ofEpochMilli(2_000L)));
        assertEquals(2_000L, repo.find(playerA, "/kit").orElseThrow().expiresAt().toEpochMilli());
    }

    @Test
    void findAllForPlayerReturnsOnlyThatPlayer() {
        repo.upsert(new CooldownEntry(playerA, "/kit", Instant.now()));
        repo.upsert(new CooldownEntry(playerA, "/home", Instant.now()));
        repo.upsert(new CooldownEntry(playerB, "/kit", Instant.now()));
        List<CooldownEntry> all = repo.findAllForPlayer(playerA);
        assertEquals(2, all.size());
        assertTrue(all.stream().allMatch(e -> e.playerId().equals(playerA)));
    }

    @Test
    void deleteRemovesSpecificEntry() {
        repo.upsert(new CooldownEntry(playerA, "/kit", Instant.now()));
        repo.upsert(new CooldownEntry(playerA, "/home", Instant.now()));
        repo.delete(playerA, "/kit");
        assertTrue(repo.find(playerA, "/kit").isEmpty());
        assertTrue(repo.find(playerA, "/home").isPresent());
    }

    @Test
    void deleteAllForPlayerWipesAllEntries() {
        repo.upsert(new CooldownEntry(playerA, "/kit", Instant.now()));
        repo.upsert(new CooldownEntry(playerA, "/home", Instant.now()));
        int removed = repo.deleteAllForPlayer(playerA);
        assertEquals(2, removed);
        assertTrue(repo.findAllForPlayer(playerA).isEmpty());
    }

    @Test
    void deleteExpiredOnlyRemovesPastEntries() {
        Instant past = Instant.now().minusSeconds(10);
        Instant future = Instant.now().plusSeconds(60);
        repo.upsert(new CooldownEntry(playerA, "/old", past));
        repo.upsert(new CooldownEntry(playerA, "/new", future));

        int removed = repo.deleteExpired(Instant.now());
        assertEquals(1, removed);
        assertFalse(repo.find(playerA, "/old").isPresent());
        assertTrue(repo.find(playerA, "/new").isPresent());
    }
}
