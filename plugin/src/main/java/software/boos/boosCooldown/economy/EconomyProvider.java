package software.boos.boosCooldown.economy;

import software.boos.boosCooldown.model.CommandData;
import org.bukkit.entity.Player;

public interface EconomyProvider {

    /** Human-readable name for log messages. */
    String name();

    /** True if provider is available (external plugin loaded, config enabled). */
    boolean isAvailable();

    /** True if command defines a cost that applies to this provider. */
    boolean appliesTo(CommandData commandData);

    /** True if the player has enough resources to pay. */
    boolean hasEnough(Player player, CommandData commandData);

    /**
     * Charge the player. Returns true on success, false if charging failed
     * (e.g. payment plugin reported an error or funds became insufficient).
     */
    boolean charge(Player player, CommandData commandData);

    /** Optional hook for reporting how much was paid / how much remains. */
    String describePayment(Player player, CommandData commandData);

    /** Optional hook for explaining why hasEnough failed. */
    String describeShortage(Player player, CommandData commandData);

    /**
     * Returns the charged amount back to the player. Default implementation
     * is a no-op for providers that cannot refund safely (e.g. XP levels that
     * were already spent). Called when {@code options.refund_on_warmup_cancel}
     * is true and a warmup is aborted.
     */
    default boolean refund(Player player, CommandData commandData) {
        return false;
    }
}
