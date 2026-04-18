package software.boos.boosCooldown.service;

import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.LimitRepository;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcLimitRepository;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimitServiceImplTest {

    private LimitRepository repo;
    private LimitServiceImpl service;
    private Player player;
    private UUID playerId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        repo = new JdbcLimitRepository(ds, StorageBackend.SQLITE);
        Executor inline = Runnable::run;
        service = new LimitServiceImpl(repo, inline);
        playerId = UUID.randomUUID();
        player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(playerId);
    }

    private CommandData commandWithLimit(int limit) {
        return CommandData.builder()
                .originalCommand("/kit")
                .commandKey("/kit")
                .player(player)
                .limit(limit)
                .build();
    }

    @Test
    void getRemainingUsesNoLimit() {
        CommandData data = CommandData.builder()
                .originalCommand("/kit").commandKey("/kit").player(player).limit(-1).build();
        assertEquals(Integer.MAX_VALUE, service.getRemainingUses(player, data));
    }

    @Test
    void getRemainingUsesReturnsConfiguredLimitWhenEmpty() {
        assertEquals(3, service.getRemainingUses(player, commandWithLimit(3)));
    }

    @Test
    void tryConsumeDecrementsUses() {
        CommandData data = commandWithLimit(3);
        assertTrue(service.tryConsume(player, data));
        assertEquals(2, service.getRemainingUses(player, data));
    }

    @Test
    void tryConsumeFailsWhenDepleted() {
        CommandData data = commandWithLimit(1);
        assertTrue(service.tryConsume(player, data));
        assertFalse(service.tryConsume(player, data));
    }

    @Test
    void tryConsumeRespectsSharedLimits() {
        CommandData data = CommandData.builder()
                .originalCommand("/kit")
                .commandKey("/kit")
                .player(player)
                .limit(3)
                .sharedLimits(List.of("/home"))
                .build();
        assertTrue(service.tryConsume(player, data));
        // shared should also be decremented
        assertEquals(2, repo.find(playerId, "/home").orElseThrow().remainingUses());
    }

    @Test
    void limitRefreshesOnceResetAtExpires() {
        CommandData data = CommandData.builder()
                .originalCommand("/daily").commandKey("/daily").player(player)
                .limit(2).limitResetDelaySeconds(60).build();

        // Burn both uses — resetAt is set on the first consume
        assertTrue(service.tryConsume(player, data));
        assertTrue(service.tryConsume(player, data));
        assertFalse(service.tryConsume(player, data), "Fully-consumed limit must block");

        // Simulate the reset window passing by rewriting the record with a past resetAt.
        repo.upsert(new software.boos.boosCooldown.model.LimitEntry(
                playerId, "/daily", 0, java.time.Instant.now().minusSeconds(1)));

        // Next read should refresh the counter back to the configured limit.
        assertEquals(2, service.getRemainingUses(player, data),
                "Expired window must restore the full limit");
        // And the first consume after the reset succeeds.
        assertTrue(service.tryConsume(player, data));
    }

    @Test
    void limitWithoutResetDelayNeverAutoRefreshes() {
        CommandData data = CommandData.builder()
                .originalCommand("/once").commandKey("/once").player(player)
                .limit(1).build();

        assertTrue(service.tryConsume(player, data));
        assertFalse(service.tryConsume(player, data));
        // No reset delay = no auto-refresh, ever.
        assertFalse(service.tryConsume(player, data));
    }

    @Test
    void resetUsesForPlayerClears() throws ExecutionException, InterruptedException {
        service.tryConsume(player, commandWithLimit(3));
        service.resetUsesForPlayer(player).get();
        // After reset, repo should be empty → getRemainingUses returns configured limit
        assertEquals(3, service.getRemainingUses(player, commandWithLimit(3)));
    }

    @Test
    void resetUsesForCommandClearsAcrossPlayers() throws Exception {
        UUID otherId = UUID.randomUUID();
        Player otherPlayer = Mockito.mock(Player.class);
        Mockito.when(otherPlayer.getUniqueId()).thenReturn(otherId);

        service.tryConsume(player, commandWithLimit(2));
        service.tryConsume(otherPlayer, CommandData.builder()
                .originalCommand("/kit").commandKey("/kit").player(otherPlayer).limit(2).build());

        service.resetUsesForCommand("/kit").get();
        assertEquals(2, service.getRemainingUses(player, commandWithLimit(2)));
    }
}
