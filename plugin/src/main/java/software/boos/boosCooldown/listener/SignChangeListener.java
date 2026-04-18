package software.boos.boosCooldown.listener;

import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

public final class SignChangeListener implements Listener {

    private static final String MARKER = "[boosCooldowns]";

    private final MessageConfig messages;

    public SignChangeListener(MessageConfig messages) {
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!MARKER.equals(event.getLine(0))) return;

        Player player = event.getPlayer();
        String line2 = event.getLine(1);
        if (line2 == null) return;

        boolean allowed = switch (line2) {
            case "player" -> player.hasPermission("booscooldowns.signs.player.place");
            case "server" -> player.hasPermission("booscooldowns.signs.server.place");
            default -> true;
        };
        if (!allowed) {
            BoosChat.sendMessage(player, messages.cannotCreateSign());
            event.getBlock().breakNaturally();
            event.setCancelled(true);
        }
    }
}
