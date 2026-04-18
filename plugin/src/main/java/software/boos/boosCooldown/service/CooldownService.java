package software.boos.boosCooldown.service;

import software.boos.boosCooldown.model.CommandData;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface CooldownService {

    boolean isOnCooldown(Player player, CommandData commandData);

    long getRemainingCooldown(Player player, CommandData commandData);

    boolean isServerOnCooldown(CommandData commandData);

    long getRemainingServerCooldown(CommandData commandData);

    CompletableFuture<Void> setCooldown(Player player, CommandData commandData);

    CompletableFuture<Void> setServerCooldown(CommandData commandData);

    CompletableFuture<Void> removeCooldown(Player player, CommandData commandData);

    CompletableFuture<Void> removeCooldown(Player player, String commandKey);

    CompletableFuture<Void> resetCooldowns(Player player);

    /**
     * Prefetches all persisted cooldowns for a player into the in-memory
     * cache. Called at join time so the hot-path never has to block on JDBC.
     */
    CompletableFuture<Void> prefetch(Player player);

    /** Drops cached entries for a player (e.g. on quit). */
    void forget(Player player);
}
