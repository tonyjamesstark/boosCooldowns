package software.boos.boosCooldown.service;

import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.AuditEntry;
import software.boos.boosCooldown.persistence.repository.AuditRepository;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Thin async wrapper around {@link AuditRepository}. Every command decision
 * the pipeline makes produces one audit row — but on the hot path we just
 * dispatch a task to the plugin's async executor so the write never blocks
 * the main thread.
 */
public final class AuditService {

    private final AuditRepository repository;
    private final PluginConfig config;
    private final Executor asyncExecutor;

    public AuditService(AuditRepository repository, PluginConfig config, Executor asyncExecutor) {
        this.repository = repository;
        this.config = config;
        this.asyncExecutor = asyncExecutor;
    }

    public void record(Player player, String command, AuditEntry.Outcome outcome) {
        if (!config.isAuditEnabled()) return;
        asyncExecutor.execute(() -> repository.record(
                Instant.now(), player.getUniqueId(), player.getName(), command, outcome.name()));
    }

    public List<AuditEntry> recent(int limit) {
        return repository.findRecent(limit);
    }

    public List<AuditEntry> forCommand(String command, int limit) {
        return repository.findForCommand(command, limit);
    }

    public int purgeOlderThan(Instant cutoff) {
        return repository.deleteOlderThan(cutoff);
    }
}
