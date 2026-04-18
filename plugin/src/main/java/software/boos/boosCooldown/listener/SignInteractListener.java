package software.boos.boosCooldown.listener;

import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

public final class SignInteractListener implements Listener {

    private static final String MARKER = "[boosCooldowns]";

    private final Plugin plugin;
    private final MessageConfig messages;

    public SignInteractListener(Plugin plugin, MessageConfig messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.hasBlock()) return;
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) return;

        SignSide side = sign.getSide(Side.FRONT);
        String line1 = side.getLine(0);
        if (!MARKER.equals(line1)) return;

        String line2 = side.getLine(1);
        String message = assembleCommand(side.getLine(2), side.getLine(3));
        Player player = event.getPlayer();

        switch (line2) {
            case "player" -> {
                if (player.hasPermission("booscooldowns.signs.player.use")) {
                    player.chat(message);
                } else {
                    BoosChat.sendMessage(player, messages.cannotUseSign());
                }
            }
            case "server" -> {
                if (player.hasPermission("booscooldowns.signs.server.use")) {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), message);
                } else {
                    BoosChat.sendMessage(player, messages.cannotUseSign());
                }
            }
            default -> BoosChat.sendMessage(player, messages.cannotUseSign());
        }
    }

    private static String assembleCommand(String line3, String line4) {
        if (line3 == null) return "";
        if (line3.endsWith("+") || (line4 != null && !line4.isEmpty())) {
            String trimmed = line3.endsWith("+") ? line3.substring(0, line3.length() - 1) : line3;
            return trimmed + " " + (line4 == null ? "" : line4);
        }
        return line3;
    }
}
