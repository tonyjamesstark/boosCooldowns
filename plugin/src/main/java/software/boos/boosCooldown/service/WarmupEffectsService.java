package software.boos.boosCooldown.service;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.CommandData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plays warmup sounds, spawns warmup particles and drives a boss-bar progress
 * indicator for the duration of a warmup. Hooks into {@link WarmupServiceImpl}
 * via start/stop calls from {@link software.boos.boosCooldown.listener.CommandPreprocessListener}.
 */
public final class WarmupEffectsService {

    private final BoosCoolDown plugin;
    private final PluginConfig config;
    private final Map<UUID, WarmupEffect> active = new ConcurrentHashMap<>();

    public WarmupEffectsService(BoosCoolDown plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void startEffects(Player player, CommandData data) {
        stopEffects(player); // ensure single active effect per player

        BossBar bar = null;
        if (config.isBossBarWarmup() && !player.hasPermission("booscooldowns.nobossbar")) {
            bar = BossBar.bossBar(Component.text("Charging " + data.originalCommand()),
                    1.0f, parseColor(config.getBossBarColor()), parseOverlay(config.getBossBarStyle()));
            player.showBossBar(bar);
        }

        playSound(player, data.warmupSound());
        BukkitTask particleTask = null;
        if (data.warmupParticle() != null && !data.warmupParticle().isEmpty()) {
            Particle particle = parseParticle(data.warmupParticle());
            if (particle != null) {
                particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (player.isOnline()) {
                        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0),
                                8, 0.4, 0.8, 0.4, 0.01);
                    }
                }, 0L, 10L);
            }
        }

        BukkitTask barTask = null;
        if (bar != null) {
            final BossBar finalBar = bar;
            final long totalTicks = Math.max(1, data.warmupSeconds() * 20L);
            final long startTick = Bukkit.getCurrentTick();
            barTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) return;
                long elapsed = Bukkit.getCurrentTick() - startTick;
                float progress = Math.max(0f, Math.min(1f, 1f - (float) elapsed / totalTicks));
                finalBar.progress(progress);
            }, 1L, 2L);
        }

        active.put(player.getUniqueId(), new WarmupEffect(bar, particleTask, barTask));
    }

    public void stopEffects(Player player) {
        WarmupEffect effect = active.remove(player.getUniqueId());
        if (effect == null) return;
        if (effect.bar != null) player.hideBossBar(effect.bar);
        if (effect.particleTask != null) effect.particleTask.cancel();
        if (effect.barTask != null) effect.barTask.cancel();
    }

    public void playCompleteSound(Player player, CommandData data) {
        stopEffects(player);
        playSound(player, data.completeSound());
    }

    public void shutdown() {
        for (var entry : active.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && entry.getValue().bar != null) {
                player.hideBossBar(entry.getValue().bar);
            }
            if (entry.getValue().particleTask != null) entry.getValue().particleTask.cancel();
            if (entry.getValue().barTask != null) entry.getValue().barTask.cancel();
        }
        active.clear();
    }

    private void playSound(Player player, String soundName) {
        if (soundName == null || soundName.isEmpty()) return;
        try {
            // Paper 1.21+ exposes Sound as a Keyed registry — valueOf() is
            // deprecated for removal. We catch LinkageError too in case a
            // future Paper version removes it outright.
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace('.', '_'));
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[boosCooldowns] Unknown sound: " + soundName);
        } catch (LinkageError e) {
            plugin.getLogger().warning("[boosCooldowns] Sound.valueOf() not available on this Paper build — "
                    + "upgrade the plugin. Sound '" + soundName + "' skipped.");
        }
    }

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[boosCooldowns] Unknown particle: " + name);
            return null;
        }
    }

    private BossBar.Color parseColor(String name) {
        try {
            return BossBar.Color.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.BLUE;
        }
    }

    private BossBar.Overlay parseOverlay(String name) {
        try {
            return BossBar.Overlay.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private record WarmupEffect(BossBar bar, BukkitTask particleTask, BukkitTask barTask) {
    }
}
