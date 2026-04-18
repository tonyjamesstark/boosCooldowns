package software.boos.boosCooldown.service;

import software.boos.boosCooldown.model.CommandData;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface LimitService {

    /**
     * Returns remaining uses for the given command. If no record exists yet,
     * returns the command's configured limit.
     */
    int getRemainingUses(Player player, CommandData commandData);

    /**
     * Attempts to consume a single use. Returns true if allowed (and persists
     * the decrement), false if the player has run out.
     */
    boolean tryConsume(Player player, CommandData commandData);

    CompletableFuture<Void> resetUsesForPlayer(Player player);

    CompletableFuture<Void> resetUsesForCommand(String commandKey);

    CompletableFuture<Void> resetUses(Player player, String commandKey);

    /**
     * Manually overwrite a player's remaining uses — used by
     * {@code /bcd grant uses}. Bypasses any reset delay.
     */
    void setUses(Player player, String commandKey, int remainingUses);
}
