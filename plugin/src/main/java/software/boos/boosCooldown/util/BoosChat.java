package software.boos.boosCooldown.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Utility class for handling chat messages in the BoosCooldowns plugin.
 */
public final class BoosChat {
    
    private BoosChat() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Sends a formatted message to a command sender.
     *
     * @param sender the command sender to send the message to
     * @param message the message to send (color codes will be translated)
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(translateColorCodes(message));
        }
    }
    
    /**
     * Sends a formatted message to a player.
     * This is a convenience method that delegates to {@link #sendMessage(CommandSender, String)}.
     *
     * @param player the player to send the message to
     * @param message the message to send (color codes will be translated)
     */
    public static void sendMessageToPlayer(Player player, String message) {
        sendMessage(player, message);
    }
    
    /**
     * Translates color codes in a string using the '&' character as the color code character.
     *
     * @param message the message to translate color codes in
     * @return the message with color codes translated
     */
    public static String translateColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Sends an error message to a command sender.
     *
     * @param sender the command sender to send the error message to
     * @param message the error message to send (will be prefixed with red color)
     */
    public static void sendError(CommandSender sender, String message) {
        sendMessage(sender, "&c" + message);
    }
    
    /**
     * Sends a success message to a command sender.
     *
     * @param sender the command sender to send the success message to
     * @param message the success message to send (will be prefixed with green color)
     */
    public static void sendSuccess(CommandSender sender, String message) {
        sendMessage(sender, "&a" + message);
    }
    
    /**
     * Sends a warning message to a command sender.
     *
     * @param sender the command sender to send the warning message to
     * @param message the warning message to send (will be prefixed with yellow color)
     */
    public static void sendWarning(CommandSender sender, String message) {
        sendMessage(sender, "&e" + message);
    }
    
    /**
     * Sends an informational message to a command sender.
     *
     * @param sender the command sender to send the info message to
     * @param message the info message to send (will be prefixed with aqua color)
     */
    public static void sendInfo(CommandSender sender, String message) {
        sendMessage(sender, "&b" + message);
    }
}
