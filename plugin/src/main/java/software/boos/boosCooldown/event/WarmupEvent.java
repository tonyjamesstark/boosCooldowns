package software.boos.boosCooldown.event;

import software.boos.boosCooldown.model.CommandData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a player starts or completes a command warmup.
 */
public class WarmupEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final CommandData commandData;
    private final long durationSeconds;
    private boolean cancelled;
    private String cancelMessage;
    private final boolean isStarting;

    /**
     * Creates a new WarmupEvent.
     *
     * @param player the player starting or completing the warmup
     * @param commandData the command data
     * @param durationSeconds the duration of the warmup in seconds
     * @param isStarting true if the warmup is starting, false if completing
     */
    public WarmupEvent(Player player, CommandData commandData, long durationSeconds, boolean isStarting) {
        this.player = player;
        this.commandData = commandData;
        this.durationSeconds = durationSeconds;
        this.isStarting = isStarting;
        this.cancelled = false;
    }

    /**
     * Gets the player who triggered the warmup.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the command data associated with this warmup.
     *
     * @return the command data
     */
    public CommandData getCommandData() {
        return commandData;
    }

    /**
     * Gets the duration of the warmup in seconds.
     *
     * @return the duration in seconds
     */
    public long getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Checks if this event is for the start of a warmup.
     *
     * @return true if the warmup is starting, false if completing
     */
    public boolean isStarting() {
        return isStarting;
    }

    /**
     * Gets the message to send if the warmup is cancelled.
     *
     * @return the cancel message, or null for default
     */
    public String getCancelMessage() {
        return cancelMessage;
    }

    /**
     * Sets a custom message to send if the warmup is cancelled.
     *
     * @param cancelMessage the message to send, or null for default
     */
    public void setCancelMessage(String cancelMessage) {
        this.cancelMessage = cancelMessage;
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
