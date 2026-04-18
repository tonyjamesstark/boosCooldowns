package software.boos.boosCooldown.service;

import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcCooldownRepository;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcServerCooldownRepository;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownServiceImplTest {

    private CooldownServiceImpl service;
    private Player player;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        service = new CooldownServiceImpl(
                new JdbcCooldownRepository(ds, StorageBackend.SQLITE),
                new JdbcServerCooldownRepository(ds, StorageBackend.SQLITE),
                1_000L, Runnable::run);
        player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    private CommandData cd(long cooldown, long serverCooldown) {
        return CommandData.builder()
                .originalCommand("/kit")
                .commandKey("/kit")
                .player(player)
                .cooldownSeconds(cooldown)
                .serverCooldownSeconds(serverCooldown)
                .build();
    }

    @Test
    void noCooldownConfiguredIsNeverOnCooldown() {
        assertFalse(service.isOnCooldown(player, cd(0, 0)));
    }

    @Test
    void setAndIsOnCooldown() throws Exception {
        CommandData data = cd(60, 0);
        service.setCooldown(player, data).get();
        assertTrue(service.isOnCooldown(player, data));
        assertTrue(service.getRemainingCooldown(player, data) > 0);
    }

    @Test
    void expiredCooldownIsNotActive() throws Exception {
        CommandData data = cd(1, 0);
        service.setCooldown(player, data).get();
        Thread.sleep(1100);
        assertFalse(service.isOnCooldown(player, data));
    }

    @Test
    void removeCooldownClears() throws Exception {
        CommandData data = cd(60, 0);
        service.setCooldown(player, data).get();
        service.removeCooldown(player, data).get();
        assertFalse(service.isOnCooldown(player, data));
    }

    @Test
    void resetCooldownsClearsAll() throws Exception {
        CommandData kit = cd(60, 0);
        CommandData home = CommandData.builder()
                .originalCommand("/home").commandKey("/home").player(player)
                .cooldownSeconds(60).build();
        service.setCooldown(player, kit).get();
        service.setCooldown(player, home).get();
        service.resetCooldowns(player).get();
        assertFalse(service.isOnCooldown(player, kit));
        assertFalse(service.isOnCooldown(player, home));
    }

    @Test
    void sharedCooldownsAreAlsoSet() throws Exception {
        CommandData data = CommandData.builder()
                .originalCommand("/kit").commandKey("/kit").player(player)
                .cooldownSeconds(60).sharedCooldowns(List.of("/bonus")).build();
        service.setCooldown(player, data).get();

        CommandData bonus = CommandData.builder()
                .originalCommand("/bonus").commandKey("/bonus").player(player)
                .cooldownSeconds(60).build();
        assertTrue(service.isOnCooldown(player, bonus));
    }

    @Test
    void serverCooldownIsSharedAcrossPlayers() throws Exception {
        CommandData data = cd(0, 30);
        service.setServerCooldown(data).get();
        assertTrue(service.isServerOnCooldown(data));
        assertTrue(service.getRemainingServerCooldown(data) > 0);
        assertTrue(service.getRemainingServerCooldown(data) <= 30);

        // Another player sees it too (same commandKey)
        Player another = Mockito.mock(Player.class);
        Mockito.when(another.getUniqueId()).thenReturn(UUID.randomUUID());
        CommandData sameCmd = CommandData.builder()
                .originalCommand("/kit").commandKey("/kit").player(another)
                .serverCooldownSeconds(30).build();
        assertTrue(service.isServerOnCooldown(sameCmd));
    }

    @Test
    void serverCooldownUnconfiguredIsNotActive() {
        assertFalse(service.isServerOnCooldown(cd(60, 0)));
        assertEquals(0, service.getRemainingServerCooldown(cd(60, 0)));
    }
}
