package software.boos.boosCooldown.event;

import software.boos.boosCooldown.model.CommandData;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a command is being processed by the cooldown system.
 * This event is called before any cooldown or warmup checks are performed.
 */
public class CommandEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final CommandSender sender;
    private final CommandData commandData;
    private final String[] args;
    private boolean cancelled;
    private String denyMessage;

    /**
     * Creates a new CommandEvent.
     *
     * @param sender the command sender
     * @param commandData the command data
     * @param args the command arguments
     */
    public CommandEvent(CommandSender sender, CommandData commandData, String[] args) {
        this.sender = sender;
        this.commandData = commandData;
        this.args = args != null ? args.clone() : new String[0];
        this.cancelled = false;
    }

    /**
     * Gets the command sender.
     *
     * @return the command sender
     */
    public CommandSender getSender() {
        return sender;
    }

    /**
     * Gets the command data.
     *
     * @return the command data
     */
    public CommandData getCommandData() {
        return commandData;
    }

    /**
     * Gets the command arguments.
     *
     * @return a copy of the command arguments array
     */
    public String[] getArgs() {
        return args.clone();
    }

    /**
     * Gets the custom deny message that will be sent to the sender if the event is cancelled.
     *
     * @return the deny message, or null if using default message
     */
    public String getDenyMessage() {
        return denyMessage;
    }

    /**
     * Sets a custom deny message to be sent to the sender if the event is cancelled.
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
