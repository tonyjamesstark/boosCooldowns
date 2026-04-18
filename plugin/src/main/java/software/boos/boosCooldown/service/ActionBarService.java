package software.boos.boosCooldown.service;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.util.BoosChat;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Periodically updates the action bar for players with active warmups. The
 * task is self-starting/stopping based on config flags to avoid needless ticks.
 */
public final class ActionBarService {

    private final BoosCoolDown plugin;
    private final PluginConfig config;
    private final WarmupServiceImpl warmupService;
    private BukkitTask task;

    public ActionBarService(BoosCoolDown plugin, PluginConfig config, WarmupServiceImpl warmupService) {
        this.plugin = plugin;
        this.config = config;
        this.warmupService = warmupService;
    }

    public void start() {
        stop();
        if (!config.isActionBarWarmup()) {
            return;
        }
        long interval = Math.max(1, config.getActionBarUpdateIntervalTicks());
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (var warmup : warmupService.activeWarmups()) {
            Player player = warmup.getPlayer();
            if (player == null || !player.isOnline()) continue;
            if (player.hasPermission("booscooldowns.noactionbar")) continue;
            long remaining = warmup.remainingSeconds();
            sendBar(player, "&eCharging &b" + warmup.getCommand() + "&7... &f" + remaining + "s");
        }
    }

    public void sendCooldownNotice(Player player, String message) {
        if (!config.isActionBarCooldownOnAttempt()) return;
        if (player.hasPermission("booscooldowns.noactionbar")) return;
        sendBar(player, message);
    }

    private static void sendBar(Player player, String rawMessage) {
        String formatted = BoosChat.translateColorCodes(rawMessage);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(formatted));
    }
}
