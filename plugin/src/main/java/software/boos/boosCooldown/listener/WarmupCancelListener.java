package software.boos.boosCooldown.listener;

import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.service.WarmupService;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/**
 * Single listener handling every possible cause of warmup cancellation, gated
 * per-event by config flags. Replaces the fan-out of legacy
 * Boos*Move/Sneak/Sprint/Damage/GameModeChange listeners.
 */
public final class WarmupCancelListener implements Listener {

    private final PluginConfig config;
    private final MessageConfig messages;
    private final WarmupService warmups;

    public WarmupCancelListener(PluginConfig config, MessageConfig messages, WarmupService warmups) {
        this.config = config;
        this.messages = messages;
        this.warmups = warmups;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!config.isCancelWarmUpOnMove()) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        cancel(event.getPlayer(), "booscooldowns.nocancel.move", messages.warmupCancelledByMove());
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!config.isCancelWarmUpOnSneak()) return;
        cancel(event.getPlayer(), "booscooldowns.nocancel.sneak", messages.warmupCancelledBySneak());
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent event) {
        if (!config.isCancelWarmUpOnSprint()) return;
        cancel(event.getPlayer(), "booscooldowns.nocancel.sprint", messages.warmupCancelledBySprint());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!config.isCancelWarmUpOnDamage()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        cancel(player, "booscooldowns.nocancel.damage", messages.warmupCancelledByDamage());
    }

    @EventHandler
    public void onGameMode(PlayerGameModeChangeEvent event) {
        if (!config.isCancelWarmUpOnGameModeChange()) return;
        cancel(event.getPlayer(), "booscooldowns.nocancel.gamemodechange",
                messages.warmupCancelledByGameModeChange());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!config.isBlockInteractDuringWarmup()) return;
        Player player = event.getPlayer();
        if (!warmups.hasWarmup(player)) return;
        if (player.hasPermission("booscooldowns.dontblock.interact")) return;
        if (event.getAction() == Action.PHYSICAL) return;
        event.setCancelled(true);
        BoosChat.sendMessage(player, messages.interactBlocked());
    }

    private void cancel(Player player, String bypassPermission, String cancelMessage) {
        if (player.hasPermission(bypassPermission)) return;
        if (!warmups.hasWarmup(player)) return;
        warmups.cancelWarmups(player);
        BoosChat.sendMessage(player, cancelMessage);
    }
}
