package software.boos.boosCooldown.listener;

import software.boos.boosCooldown.service.CooldownService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Warms the in-memory cache on join and evicts it on quit so the
 * {@code PlayerCommandPreprocessEvent} hot-path never has to fall back
 * to a synchronous JDBC lookup.
 */
public final class PlayerConnectionListener implements Listener {

    private final CooldownService cooldowns;

    public PlayerConnectionListener(CooldownService cooldowns) {
        this.cooldowns = cooldowns;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        cooldowns.prefetch(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        cooldowns.forget(event.getPlayer());
    }
}
