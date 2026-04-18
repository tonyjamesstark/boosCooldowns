package software.boos.boosCooldown.service;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.persistence.repository.GlobalLimitResetRepository;
import software.boos.boosCooldown.persistence.repository.LimitRepository;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Persists and fires scheduled global limit resets. On plugin enable
 * reschedules everything persisted; runs due resets immediately.
 */
public final class ScheduledResetService {

    private final BoosCoolDown plugin;
    private final GlobalLimitResetRepository resetRepo;
    private final LimitRepository limitRepo;
    private final Executor asyncExecutor;
    private final Map<String, BukkitTask> tasks = new HashMap<>();

    public ScheduledResetService(BoosCoolDown plugin,
                                 GlobalLimitResetRepository resetRepo,
                                 LimitRepository limitRepo,
                                 Executor asyncExecutor) {
        this.plugin = plugin;
        this.resetRepo = resetRepo;
        this.limitRepo = limitRepo;
        this.asyncExecutor = asyncExecutor;
    }

    public void scheduleAllPersisted() {
        Map<String, Instant> pending = resetRepo.findAll();
        Instant now = Instant.now();
        for (Map.Entry<String, Instant> entry : pending.entrySet()) {
            if (!entry.getValue().isAfter(now)) {
                runReset(entry.getKey());
            } else {
                scheduleInternal(entry.getKey(), entry.getValue());
            }
        }
    }

    public CompletableFuture<Void> schedule(String commandKey, Instant resetAt) {
        return CompletableFuture.runAsync(() -> {
            resetRepo.schedule(commandKey, resetAt);
            Bukkit.getScheduler().runTask(plugin, () -> scheduleInternal(commandKey, resetAt));
        }, asyncExecutor);
    }

    public CompletableFuture<Void> cancel(String commandKey) {
        return CompletableFuture.runAsync(() -> {
            resetRepo.delete(commandKey);
            Bukkit.getScheduler().runTask(plugin, () -> {
                BukkitTask task = tasks.remove(commandKey);
                if (task != null) task.cancel();
            });
        }, asyncExecutor);
    }

    public Map<String, Instant> pending() {
        return resetRepo.findAll();
    }

    public void shutdown() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }

    private void scheduleInternal(String commandKey, Instant resetAt) {
        BukkitTask existing = tasks.remove(commandKey);
        if (existing != null) existing.cancel();
        long delayTicks = Math.max(1L, (resetAt.toEpochMilli() - System.currentTimeMillis()) / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> runReset(commandKey), delayTicks);
        tasks.put(commandKey, task);
    }

    private void runReset(String commandKey) {
        CompletableFuture.runAsync(() -> {
            limitRepo.deleteAllForCommand(commandKey);
            resetRepo.delete(commandKey);
        }, asyncExecutor);
        tasks.remove(commandKey);
        plugin.getLogger().info("[boosCooldowns] Executed scheduled global reset for " + commandKey);
    }
}
