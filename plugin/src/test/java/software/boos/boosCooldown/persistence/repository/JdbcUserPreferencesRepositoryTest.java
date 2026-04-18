package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcUserPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcUserPreferencesRepositoryTest {

    private UserPreferencesRepository repo;
    private UUID player;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        repo = new JdbcUserPreferencesRepository(ds, StorageBackend.SQLITE);
        player = UUID.randomUUID();
    }

    @Test
    void missingPreferenceReturnsDefault() {
        assertTrue(repo.getBoolean(player, "confirmations", true));
        assertFalse(repo.getBoolean(player, "confirmations", false));
    }

    @Test
    void setBooleanPersists() {
        repo.setBoolean(player, "confirmations", false);
        assertFalse(repo.getBoolean(player, "confirmations", true));
    }

    @Test
    void upsertOverwritesValue() {
        repo.upsert(player, "k", "v1");
        repo.upsert(player, "k", "v2");
        assertEquals("v2", repo.find(player, "k").orElseThrow());
    }

    @Test
    void deleteRemovesPreference() {
        repo.upsert(player, "k", "v");
        repo.delete(player, "k");
        assertTrue(repo.find(player, "k").isEmpty());
    }

    @Test
    void preferencesAreIsolatedPerPlayer() {
        UUID other = UUID.randomUUID();
        repo.setBoolean(player, "confirmations", false);
        // other player still has default
        assertTrue(repo.getBoolean(other, "confirmations", true));
    }
}
