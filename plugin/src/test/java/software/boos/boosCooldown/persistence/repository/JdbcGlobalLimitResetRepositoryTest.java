package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcGlobalLimitResetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcGlobalLimitResetRepositoryTest {

    private GlobalLimitResetRepository repo;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        repo = new JdbcGlobalLimitResetRepository(ds, StorageBackend.SQLITE);
    }

    @Test
    void scheduleStoresEntry() {
        Instant at = Instant.ofEpochMilli(9_999L);
        repo.schedule("/daily", at);
        assertEquals(at, repo.find("/daily").orElseThrow());
    }

    @Test
    void findAllReturnsEverything() {
        repo.schedule("/a", Instant.ofEpochMilli(1));
        repo.schedule("/b", Instant.ofEpochMilli(2));
        Map<String, Instant> all = repo.findAll();
        assertEquals(2, all.size());
        assertEquals(Instant.ofEpochMilli(1), all.get("/a"));
    }

    @Test
    void findDueReturnsOnlyPast() {
        repo.schedule("/past", Instant.ofEpochMilli(1));
        repo.schedule("/future", Instant.now().plusSeconds(3600));
        List<String> due = repo.findDue(Instant.now());
        assertEquals(1, due.size());
        assertEquals("/past", due.get(0));
    }

    @Test
    void deleteRemovesEntry() {
        repo.schedule("/cmd", Instant.ofEpochMilli(1));
        repo.delete("/cmd");
        assertTrue(repo.find("/cmd").isEmpty());
    }
}
