package software.boos.boosCooldown.benchmark;

import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcCooldownRepository;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcServerCooldownRepository;
import software.boos.boosCooldown.service.CooldownServiceImpl;
import org.bukkit.entity.Player;
import org.mockito.Mockito;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Hot-path micro-benchmark for the {@code isOnCooldown} lookup. Run with:
 *
 * <pre>
 *   mvn -pl plugin -Dtest=CooldownPipelineBenchmark#runBenchmark test
 * </pre>
 *
 * Or launch {@link #main(String[])} directly from an IDE. Default settings
 * exercise both the in-memory cache hit path and the DB fallback.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xms512m", "-Xmx512m"})
public class CooldownPipelineBenchmark {

    private CooldownServiceImpl service;
    private Player player;
    private CommandData cachedCommand;
    private CommandData uncachedCommand;

    @Setup
    public void setUp() throws Exception {
        Path tempDir = Files.createTempDirectory("boos-bench");
        var ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        service = new CooldownServiceImpl(
                new JdbcCooldownRepository(ds, StorageBackend.SQLITE),
                new JdbcServerCooldownRepository(ds, StorageBackend.SQLITE),
                30_000L, Runnable::run);
        player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        cachedCommand = CommandData.builder()
                .originalCommand("/cached").commandKey("/cached").player(player)
                .cooldownSeconds(60).build();
        // Prime the cache
        service.setCooldown(player, cachedCommand).join();

        uncachedCommand = CommandData.builder()
                .originalCommand("/uncached").commandKey("/uncached").player(player)
                .cooldownSeconds(60).build();
    }

    @Benchmark
    public boolean cacheHit() {
        return service.isOnCooldown(player, cachedCommand);
    }

    @Benchmark
    public boolean cacheMiss() {
        // Force miss by querying a never-stored command
        return service.isOnCooldown(player, uncachedCommand);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CooldownPipelineBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
