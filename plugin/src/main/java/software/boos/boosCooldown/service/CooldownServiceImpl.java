package software.boos.boosCooldown.service;

import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.model.CooldownEntry;
import software.boos.boosCooldown.model.ServerCooldownEntry;
import software.boos.boosCooldown.persistence.repository.CooldownRepository;
import software.boos.boosCooldown.persistence.repository.ServerCooldownRepository;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class CooldownServiceImpl implements CooldownService {

    private final CooldownRepository cooldownRepo;
    private final ServerCooldownRepository serverRepo;
    private final CachingService<String, Long> cooldownCache;
    private final CachingService<String, Long> serverCache;
    private final Executor asyncExecutor;

    public CooldownServiceImpl(CooldownRepository cooldownRepo,
                               ServerCooldownRepository serverRepo,
                               long cacheTtlMs,
                               Executor asyncExecutor) {
        this.cooldownRepo = cooldownRepo;
        this.serverRepo = serverRepo;
        this.cooldownCache = new CachingService<>(cacheTtlMs);
        this.serverCache = new CachingService<>(cacheTtlMs);
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public boolean isOnCooldown(Player player, CommandData commandData) {
        if (!commandData.hasCooldown()) {
            return false;
        }
        Long expiresAt = loadCooldown(player.getUniqueId(), commandData.commandKey());
        return expiresAt != null && expiresAt > System.currentTimeMillis();
    }

    @Override
    public long getRemainingCooldown(Player player, CommandData commandData) {
        if (!commandData.hasCooldown()) {
            return 0;
        }
        Long expiresAt = loadCooldown(player.getUniqueId(), commandData.commandKey());
        if (expiresAt == null) {
            return 0;
        }
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    @Override
    public boolean isServerOnCooldown(CommandData commandData) {
        if (!commandData.hasServerCooldown()) {
            return false;
        }
        Long expiresAt = loadServerCooldown(commandData.commandKey());
        return expiresAt != null && expiresAt > System.currentTimeMillis();
    }

    @Override
    public long getRemainingServerCooldown(CommandData commandData) {
        if (!commandData.hasServerCooldown()) {
            return 0;
        }
        Long expiresAt = loadServerCooldown(commandData.commandKey());
        if (expiresAt == null) {
            return 0;
        }
        long remaining = (expiresAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    @Override
    public CompletableFuture<Void> setCooldown(Player player, CommandData commandData) {
        if (!commandData.hasCooldown()) {
            return CompletableFuture.completedFuture(null);
        }
        UUID uuid = player.getUniqueId();
        long cooldownSeconds = applyProgressiveMultiplier(uuid, commandData);
        long expiresAt = System.currentTimeMillis() + cooldownSeconds * 1000L;
        return CompletableFuture.runAsync(() -> {
            cooldownRepo.upsert(new CooldownEntry(uuid, commandData.commandKey(), Instant.ofEpochMilli(expiresAt)));
            cooldownCache.put(cacheKey(uuid, commandData.commandKey()), expiresAt);
            for (String shared : commandData.sharedCooldowns()) {
                String key = shared.toLowerCase();
                cooldownRepo.upsert(new CooldownEntry(uuid, key, Instant.ofEpochMilli(expiresAt)));
                cooldownCache.put(cacheKey(uuid, key), expiresAt);
            }
        }, asyncExecutor);
    }

    /**
     * Progressive cooldown state keyed by (UUID, commandKey). Tracks how many
     * consecutive uses the player has made within the configured reset window.
     * The map is in-memory — not persisted — because the "streak" is a
     * short-term anti-abuse measure, not permanent state.
     */
    private final java.util.concurrent.ConcurrentHashMap<String, ProgressiveState> progressive =
            new java.util.concurrent.ConcurrentHashMap<>();

    private long applyProgressiveMultiplier(UUID uuid, CommandData data) {
        if (!data.hasProgressiveCooldown()) return data.cooldownSeconds();
        String key = uuid + "|" + data.commandKey();
        long now = System.currentTimeMillis();
        ProgressiveState state = progressive.compute(key, (k, existing) -> {
            if (existing == null || now - existing.lastUseMillis > data.cooldownMultiplierResetSeconds() * 1000L) {
                return new ProgressiveState(1, now);
            }
            return new ProgressiveState(existing.streak + 1, now);
        });
        double multiplier = Math.pow(data.cooldownMultiplier(), state.streak - 1);
        return (long) (data.cooldownSeconds() * multiplier);
    }

    private record ProgressiveState(int streak, long lastUseMillis) {
    }

    @Override
    public CompletableFuture<Void> setServerCooldown(CommandData commandData) {
        if (!commandData.hasServerCooldown()) {
            return CompletableFuture.completedFuture(null);
        }
        long expiresAt = System.currentTimeMillis() + commandData.serverCooldownSeconds() * 1000L;
        return CompletableFuture.runAsync(() -> {
            serverRepo.upsert(new ServerCooldownEntry(commandData.commandKey(), Instant.ofEpochMilli(expiresAt)));
            serverCache.put(commandData.commandKey(), expiresAt);
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> removeCooldown(Player player, CommandData commandData) {
        return removeCooldown(player, commandData.commandKey());
    }

    @Override
    public CompletableFuture<Void> removeCooldown(Player player, String commandKey) {
        UUID uuid = player.getUniqueId();
        return CompletableFuture.runAsync(() -> {
            cooldownRepo.delete(uuid, commandKey);
            cooldownCache.remove(cacheKey(uuid, commandKey));
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> resetCooldowns(Player player) {
        UUID uuid = player.getUniqueId();
        return CompletableFuture.runAsync(() -> {
            cooldownRepo.deleteAllForPlayer(uuid);
            String prefix = uuid + "|";
            cooldownCache.removeIf(k -> k.startsWith(prefix));
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> prefetch(Player player) {
        UUID uuid = player.getUniqueId();
        return CompletableFuture.runAsync(() -> {
            long now = System.currentTimeMillis();
            for (var entry : cooldownRepo.findAllForPlayer(uuid)) {
                long expires = entry.expiresAt().toEpochMilli();
                if (expires > now) {
                    cooldownCache.put(cacheKey(uuid, entry.commandKey()), expires);
                }
            }
        }, asyncExecutor);
    }

    @Override
    public void forget(Player player) {
        String prefix = player.getUniqueId() + "|";
        cooldownCache.removeIf(k -> k.startsWith(prefix));
    }

    public void cleanUpCaches() {
        cooldownCache.cleanUp();
        serverCache.cleanUp();
    }

    private Long loadCooldown(UUID playerId, String commandKey) {
        return cooldownCache.get(cacheKey(playerId, commandKey), k -> {
            Optional<CooldownEntry> entry = cooldownRepo.find(playerId, commandKey);
            if (entry.isEmpty()) return null;
            long millis = entry.get().expiresAt().toEpochMilli();
            return millis > System.currentTimeMillis() ? millis : null;
        });
    }

    private Long loadServerCooldown(String commandKey) {
        return serverCache.get(commandKey, k -> {
            Optional<ServerCooldownEntry> entry = serverRepo.find(commandKey);
            if (entry.isEmpty()) return null;
            long millis = entry.get().expiresAt().toEpochMilli();
            return millis > System.currentTimeMillis() ? millis : null;
        });
    }

    private static String cacheKey(UUID playerId, String commandKey) {
        return playerId + "|" + commandKey;
    }
}
