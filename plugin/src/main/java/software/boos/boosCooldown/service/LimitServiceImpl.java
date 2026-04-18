package software.boos.boosCooldown.service;

import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.model.LimitEntry;
import software.boos.boosCooldown.persistence.repository.LimitRepository;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class LimitServiceImpl implements LimitService {

    private final LimitRepository repository;
    private final Executor asyncExecutor;

    public LimitServiceImpl(LimitRepository repository, Executor asyncExecutor) {
        this.repository = repository;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public int getRemainingUses(Player player, CommandData commandData) {
        if (!commandData.hasLimit()) {
            return Integer.MAX_VALUE;
        }
        Optional<LimitEntry> entry = repository.find(player.getUniqueId(), commandData.commandKey());
        if (entry.isEmpty()) return commandData.limit();
        LimitEntry fresh = refreshIfExpired(entry.get(), commandData);
        return fresh.remainingUses();
    }

    @Override
    public boolean tryConsume(Player player, CommandData commandData) {
        if (!commandData.hasLimit()) {
            return true;
        }
        UUID uuid = player.getUniqueId();
        Optional<LimitEntry> stored = repository.find(uuid, commandData.commandKey());
        LimitEntry entry = stored.map(e -> refreshIfExpired(e, commandData)).orElse(null);
        int remaining = entry != null ? entry.remainingUses() : commandData.limit();
        if (remaining <= 0) {
            return false;
        }
        Instant resetAt = entry != null ? entry.resetAt() : null;
        // Fixed window: set resetAt on the *first* use and keep it until it fires.
        if (resetAt == null && commandData.limitResetDelaySeconds() > 0) {
            resetAt = Instant.now().plusSeconds(commandData.limitResetDelaySeconds());
        }
        LimitEntry updated = new LimitEntry(uuid, commandData.commandKey(), remaining - 1, resetAt);
        repository.upsert(updated);
        for (String shared : commandData.sharedLimits()) {
            String key = shared.toLowerCase();
            Optional<LimitEntry> sharedStored = repository.find(uuid, key);
            LimitEntry sharedEntry = sharedStored.map(e -> refreshIfExpired(e, commandData)).orElse(null);
            int sharedRemaining = sharedEntry != null ? sharedEntry.remainingUses() : commandData.limit();
            if (sharedRemaining > 0) {
                Instant sharedReset = sharedEntry != null ? sharedEntry.resetAt() : resetAt;
                repository.upsert(new LimitEntry(uuid, key, sharedRemaining - 1, sharedReset));
            }
        }
        return true;
    }

    /**
     * If {@code entry.resetAt} is already in the past, replace it with a fresh
     * record at the full configured limit (and {@code null} resetAt so the
     * next consume starts a new window). Writes the refreshed record back so
     * other servers sharing the DB see it too.
     */
    private LimitEntry refreshIfExpired(LimitEntry entry, CommandData commandData) {
        Instant resetAt = entry.resetAt();
        if (resetAt == null || resetAt.isAfter(Instant.now())) {
            return entry;
        }
        LimitEntry refreshed = new LimitEntry(entry.playerId(), entry.commandKey(),
                commandData.limit(), null);
        repository.upsert(refreshed);
        return refreshed;
    }

    @Override
    public CompletableFuture<Void> resetUsesForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        return CompletableFuture.runAsync(() -> repository.deleteAllForPlayer(uuid), asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> resetUsesForCommand(String commandKey) {
        return CompletableFuture.runAsync(() -> repository.deleteAllForCommand(commandKey), asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> resetUses(Player player, String commandKey) {
        UUID uuid = player.getUniqueId();
        return CompletableFuture.runAsync(() -> repository.delete(uuid, commandKey), asyncExecutor);
    }

    @Override
    public void setUses(Player player, String commandKey, int remainingUses) {
        UUID uuid = player.getUniqueId();
        asyncExecutor.execute(() -> repository.upsert(
                new software.boos.boosCooldown.model.LimitEntry(uuid, commandKey, remainingUses, null)));
    }
}
