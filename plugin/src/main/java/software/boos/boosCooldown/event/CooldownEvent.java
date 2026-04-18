package software.boos.boosCooldown.event;

import software.boos.boosCooldown.model.CommandData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a player attempts to use a command that has a cooldown.
 */
public class CooldownEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final CommandData commandData;
    private boolean cancelled;
    private String denyMessage;

    public CooldownEvent(Player player, CommandData commandData) {
        this.player = player;
        this.commandData = commandData;
        this.cancelled = false;
    }

    /**
     * Gets the player who triggered the cooldown check.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the command data associated with this cooldown.
     *
     * @return the command data
     */
    public CommandData getCommandData() {
        return commandData;
    }

    /**
     * Gets the custom deny message that will be sent to the player if the event is cancelled.
     *
     * @return the deny message, or null if using default message
     */
    public String getDenyMessage() {
        return denyMessage;
    }

    /**
     * Sets a custom deny message to be sent to the player if the event is cancelled.
     * Set to null to use the default message.
     *
     * @param denyMessage the custom deny message
     */
    public void setDenyMessage(String denyMessage) {
        this.denyMessage = denyMessage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
