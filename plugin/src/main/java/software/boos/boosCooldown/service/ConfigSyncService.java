package software.boos.boosCooldown.service;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.ConfigVersion;
import software.boos.boosCooldown.persistence.repository.ConfigVersionRepository;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Multi-server config drift detector.
 *
 * <p>YAML stays the source of truth on every node. On every enable and
 * every {@code /bcd reload} we SHA-256 hash the current {@code config.yml}
 * and upsert it into the {@code config_version} table. A periodic task then
 * scans every known node's hash and logs a warning whenever peers disagree.
 *
 * <p>The whole thing is opt-in: if the admin uses the default SQLite backend
 * there is by definition only one node, so we skip the warnings entirely.
 */
public final class ConfigSyncService {

    private final BoosCoolDown plugin;
    private final PluginConfig config;
    private final ConfigVersionRepository repository;
    private final Executor asyncExecutor;
    private final Logger logger;
    private final String nodeId;

    private BukkitTask monitorTask;
    private String lastLoggedDriftHash;

    public ConfigSyncService(BoosCoolDown plugin, PluginConfig config,
                             ConfigVersionRepository repository, Executor asyncExecutor) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.asyncExecutor = asyncExecutor;
        this.logger = plugin.getLogger();
        this.nodeId = resolveNodeId(config);
    }

    public String nodeId() {
        return nodeId;
    }

    public String currentHash() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        return hashFile(file);
    }

    /**
     * True when more than one node has published a hash — i.e. the admin is
     * actually running a multi-server setup and should worry about drift.
     */
    public boolean isMultiNode() {
        try {
            return repository.findAll().size() > 1;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public List<ConfigVersion> allNodes() {
        return repository.findAll();
    }

    /** Publish our current hash. Called on enable and on reload. */
    public void publishSelf() {
        asyncExecutor.execute(() -> {
            try {
                String hash = currentHash();
                repository.upsert(nodeId, hash, Instant.now());
                lastLoggedDriftHash = null; // allow the warning to fire again if drift re-appears
            } catch (RuntimeException e) {
                logger.warning("[boosCooldowns] Failed to publish config version: " + e.getMessage());
            }
        });
    }

    /** Start the periodic drift monitor. Idempotent. */
    public void startMonitor() {
        stopMonitor();
        long intervalTicks = Math.max(20L, 20L * config.getConfigSyncIntervalSeconds());
        monitorTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::checkDrift, intervalTicks, intervalTicks);
    }

    public void stopMonitor() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
    }

    public void shutdown() {
        stopMonitor();
        // We intentionally do NOT delete our node_id on shutdown — a crashing
        // node should still be visible in /bcd diff until someone restarts it.
    }

    private void checkDrift() {
        List<ConfigVersion> versions;
        try {
            versions = repository.findAll();
        } catch (RuntimeException e) {
            return; // transient DB hiccup — next tick will retry
        }
        if (versions.size() < 2) return;

        String myHash = currentHash();
        boolean drift = versions.stream().anyMatch(v -> !v.contentHash().equals(myHash));
        if (!drift) {
            lastLoggedDriftHash = null;
            return;
        }
        // Rate-limit: only log when the mismatch hash changes, not every tick.
        String signature = versions.stream()
                .map(v -> v.nodeId() + "=" + v.contentHash().substring(0, 8))
                .sorted()
                .reduce("", (a, b) -> a + "|" + b);
        if (signature.equals(lastLoggedDriftHash)) return;
        lastLoggedDriftHash = signature;

        logger.warning("[boosCooldowns] Config drift detected across nodes:");
        for (ConfigVersion v : versions) {
            String marker = v.nodeId().equals(nodeId) ? " (this server)" : "";
            logger.warning(String.format("  %s  hash=%s  reloaded=%s%s",
                    v.nodeId(), v.contentHash().substring(0, 8),
                    v.reloadedAt(), marker));
        }
        logger.warning("[boosCooldowns] Edit config.yml on every node and run /bcd reload, "
                + "or run /bcd diff to inspect the mismatch.");
    }

    // ---------------- helpers ----------------

    private static String resolveNodeId(PluginConfig config) {
        String explicit = config.getString("options.options.node_id", null);
        if (explicit != null && !explicit.isBlank()) return explicit;
        try {
            return InetAddress.getLocalHost().getHostName() + ":" + Bukkit.getPort();
        } catch (UnknownHostException | LinkageError e) {
            return "node-" + Objects.hashCode(Bukkit.getServer());
        }
    }

    private static String hashFile(File file) {
        try {
            byte[] bytes = file.isFile() ? Files.readAllBytes(file.toPath()) : new byte[0];
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException | java.io.IOException e) {
            return "unknown";
        }
    }
}
