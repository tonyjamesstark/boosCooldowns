package software.boos.boosCooldown.service;

import software.boos.boosCooldown.model.CommandData;
import org.bukkit.entity.Player;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for managing command warmups.
 */
public interface WarmupService {

    /**
     * Starts a warmup for a command.
     *
     * @param player the player starting the warmup
     * @param commandData the command data containing warmup information
     * @param onComplete callback to execute when warmup completes
     * @param onCancel callback to execute if warmup is cancelled
     * @return a WarmupHandle to control the warmup
     */
    WarmupHandle startWarmup(Player player, CommandData commandData, Runnable onComplete, Runnable onCancel);

    /**
     * Cancels all warmups for a player.
     *
     * @param player the player to cancel warmups for
     * @return a CompletableFuture that completes when all warmups are cancelled
     */
    CompletableFuture<Void> cancelWarmups(Player player);

    /**
     * Checks if a player has any active warmups.
     *
     * @param player the player to check
     * @return true if the player has active warmups, false otherwise
     */
    boolean hasWarmup(Player player);

    /**
     * Gets the remaining warmup time for a command.
     *
     * @param player the player to check
     * @param commandData the command data
     * @return the remaining warmup time in seconds, or 0 if no warmup
     */
    long getRemainingWarmup(Player player, CommandData commandData);

    /**
     * Handle for controlling a warmup.
     */
    interface WarmupHandle {
        /**
         * Cancels the warmup if it's still active.
         *
         * @return true if the warmup was cancelled, false if it was already completed or cancelled
         */
        boolean cancel();

        /**
         * Gets the player this warmup is for.
         *
         * @return the player
         */
        Player getPlayer();

        /**
         * Gets the command this warmup is for.
         *
         * @return the command
         */
        String getCommand();
    }
}
