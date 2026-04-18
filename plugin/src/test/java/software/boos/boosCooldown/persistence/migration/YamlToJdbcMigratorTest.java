package software.boos.boosCooldown.persistence.migration;

import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.CooldownEntry;
import software.boos.boosCooldown.model.LimitEntry;
import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.CooldownRepository;
import software.boos.boosCooldown.persistence.repository.LimitRepository;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcCooldownRepository;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcLimitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlToJdbcMigratorTest {

    @Test
    void missingUsersYamlIsNoOp(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        PluginConfig config = Mockito.mock(PluginConfig.class);
        Mockito.when(config.getGroups()).thenReturn(Set.of("default"));

        CooldownRepository cooldowns = new JdbcCooldownRepository(ds, StorageBackend.SQLITE);
        LimitRepository limits = new JdbcLimitRepository(ds, StorageBackend.SQLITE);

        new YamlToJdbcMigrator(tempDir.toFile(), config, cooldowns, limits,
                Logger.getLogger("T")).migrateIfNeeded();

        // No tables should have been touched
        assertTrue(cooldowns.findAllForPlayer(UUID.randomUUID()).isEmpty());
    }

    @Test
    void migratesLegacyCooldownsAndRenamesFile(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        UUID player = UUID.randomUUID();
        int hash = "/kit".hashCode();

        String yaml = "users:\n" +
                "  " + player + ":\n" +
                "    cooldown:\n" +
                "      '" + hash + "': '01.01.2099 12:00:00'\n" +
                "    uses:\n" +
                "      '" + hash + "': 3\n";
        Files.writeString(tempDir.resolve("users.yml"), yaml);

        PluginConfig config = Mockito.mock(PluginConfig.class);
        Mockito.when(config.getGroups()).thenReturn(Set.of("default"));
        Mockito.when(config.getCommandsForGroup("default")).thenReturn(Set.of("/kit"));

        CooldownRepository cooldowns = new JdbcCooldownRepository(ds, StorageBackend.SQLITE);
        LimitRepository limits = new JdbcLimitRepository(ds, StorageBackend.SQLITE);

        new YamlToJdbcMigrator(tempDir.toFile(), config, cooldowns, limits,
                Logger.getLogger("T")).migrateIfNeeded();

        CooldownEntry cdEntry = cooldowns.find(player, "/kit").orElseThrow();
        assertEquals("/kit", cdEntry.commandKey());

        LimitEntry limEntry = limits.find(player, "/kit").orElseThrow();
        assertEquals(3, limEntry.remainingUses());

        assertFalse(Files.exists(tempDir.resolve("users.yml")),
                "users.yml should be renamed after successful migration");
        assertTrue(Files.list(tempDir).anyMatch(p -> p.getFileName().toString().startsWith("users.yml.migrated-")));
    }

    @Test
    void migrationSkipsUnknownHashKeys(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        UUID player = UUID.randomUUID();

        String yaml = "users:\n" +
                "  " + player + ":\n" +
                "    cooldown:\n" +
                "      '12345': '01.01.2099 12:00:00'\n";
        Files.writeString(tempDir.resolve("users.yml"), yaml);

        PluginConfig config = Mockito.mock(PluginConfig.class);
        Mockito.when(config.getGroups()).thenReturn(Set.of("default"));
        Mockito.when(config.getCommandsForGroup("default")).thenReturn(Set.of("/kit"));

        CooldownRepository cooldowns = new JdbcCooldownRepository(ds, StorageBackend.SQLITE);
        LimitRepository limits = new JdbcLimitRepository(ds, StorageBackend.SQLITE);

        new YamlToJdbcMigrator(tempDir.toFile(), config, cooldowns, limits,
                Logger.getLogger("T")).migrateIfNeeded();

        assertTrue(cooldowns.findAllForPlayer(player).isEmpty());
    }
}
