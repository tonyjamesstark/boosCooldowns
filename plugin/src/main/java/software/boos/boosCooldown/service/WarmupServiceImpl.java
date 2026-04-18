package software.boos.boosCooldown.service;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.model.CommandData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class WarmupServiceImpl implements WarmupService {

    private final BoosCoolDown plugin;
    private final Map<String, WarmupTask> activeWarmups = new ConcurrentHashMap<>();

    public WarmupServiceImpl(BoosCoolDown plugin) {
        this.plugin = plugin;
    }

    @Override
    public WarmupHandle startWarmup(Player player, CommandData commandData, Runnable onComplete, Runnable onCancel) {
        if (!commandData.hasWarmup()) {
            onComplete.run();
            return new CompletedHandle(player, commandData.originalCommand());
        }

        String key = key(player.getUniqueId(), commandData.commandKey());
        WarmupTask existing = activeWarmups.remove(key);
        if (existing != null) {
            existing.cancel();
        }

        WarmupTask task = new WarmupTask(player, commandData, onComplete, onCancel, key);
        activeWarmups.put(key, task);
        long ticks = commandData.warmupSeconds() * 20L;
        task.scheduled = Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
        return task;
    }

    @Override
    public CompletableFuture<Void> cancelWarmups(Player player) {
        String prefix = player.getUniqueId() + "|";
        activeWarmups.entrySet().removeIf(e -> {
            if (e.getKey().startsWith(prefix)) {
                e.getValue().cancel();
                return true;
            }
            return false;
        });
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean hasWarmup(Player player) {
        String prefix = player.getUniqueId() + "|";
        return activeWarmups.keySet().stream().anyMatch(k -> k.startsWith(prefix));
    }

    @Override
    public long getRemainingWarmup(Player player, CommandData commandData) {
        WarmupTask task = activeWarmups.get(key(player.getUniqueId(), commandData.commandKey()));
        if (task == null || task.cancelled.get() || task.completed.get()) {
            return 0;
        }
        long remainingMs = task.endMillis - System.currentTimeMillis();
        return Math.max(0, remainingMs / 1000);
    }

    public Collection<WarmupTask> activeWarmups() {
        return activeWarmups.values();
    }

    private static String key(UUID playerId, String commandKey) {
        return playerId + "|" + commandKey;
    }

    public final class WarmupTask implements WarmupHandle, Runnable {
        private final Player player;
        private final CommandData commandData;
        private final Runnable onComplete;
        private final Runnable onCancel;
        private final String mapKey;
        private final long endMillis;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private BukkitTask scheduled;

        WarmupTask(Player player, CommandData commandData, Runnable onComplete, Runnable onCancel, String mapKey) {
            this.player = player;
            this.commandData = commandData;
            this.onComplete = onComplete;
            this.onCancel = onCancel;
            this.mapKey = mapKey;
            this.endMillis = System.currentTimeMillis() + commandData.warmupSeconds() * 1000L;
        }

        @Override
        public void run() {
            if (cancelled.get()) return;
            completed.set(true);
            activeWarmups.remove(mapKey, this);
            try {
                onComplete.run();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in warmup completion", e);
            }
        }

        @Override
        public boolean cancel() {
            if (!cancelled.compareAndSet(false, true)) return false;
            if (scheduled != null) scheduled.cancel();
            activeWarmups.remove(mapKey, this);
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    onCancel.run();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in warmup cancellation", e);
                }
            });
            return true;
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public String getCommand() {
            return commandData.originalCommand();
        }

        public CommandData commandData() {
            return commandData;
        }

        public long remainingSeconds() {
            if (cancelled.get() || completed.get()) return 0;
            return Math.max(0, (endMillis - System.currentTimeMillis()) / 1000);
        }
    }

    private record CompletedHandle(Player player, String command) implements WarmupHandle {
        @Override public boolean cancel() { return false; }
        @Override public Player getPlayer() { return player; }
        @Override public String getCommand() { return command; }
    }
}
