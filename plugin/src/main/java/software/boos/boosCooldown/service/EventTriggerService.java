package software.boos.boosCooldown.service;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.EventExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the {@code event_triggers} section of config.yml and wires up
 * listener callbacks that reset/grant cooldowns when specific Bukkit events
 * fire. Supports a minimal vocabulary of curated events (killing an entity,
 * earning an advancement) — deliberately keeps the scope narrow so config
 * writers don't need to know Bukkit internals.
 *
 * <pre>
 * event_triggers:
 *   - when: KILL
 *     entity: ENDER_DRAGON
 *     reset_cooldown: /boss_spawn
 *     message: "&6You slew the dragon! &e/boss_spawn&6 is ready again."
 *     sound: ENTITY_PLAYER_LEVELUP
 *     broadcast: false          # true = announce to every online player
 *   - when: ADVANCEMENT
 *     advancement: story/mine_diamond
 *     reset_cooldown: /reward
 *     message: "&aYour /reward cooldown has been reset."
 * </pre>
 *
 * <p>Message placeholders: {@code &command&} (the command key whose cooldown
 * was reset), {@code &player&}, {@code &entity&} (KILL triggers only),
 * {@code &advancement&} (ADVANCEMENT triggers only).
 */
public final class EventTriggerService {

    private final BoosCoolDown plugin;
    private final PluginConfig config;
    private final CooldownService cooldowns;
    private final List<Listener> registered = new ArrayList<>();

    public EventTriggerService(BoosCoolDown plugin, PluginConfig config, CooldownService cooldowns) {
        this.plugin = plugin;
        this.config = config;
        this.cooldowns = cooldowns;
    }

    public void registerAll() {
        unregisterAll();
        List<Map<?, ?>> triggers = config.raw().getMapList("event_triggers");
        for (Map<?, ?> trigger : triggers) {
            String when = String.valueOf(trigger.get("when")).toUpperCase();
            String resetCommand = String.valueOf(trigger.get("reset_cooldown"));
            switch (when) {
                case "KILL" -> registerKillListener(trigger, resetCommand);
                case "ADVANCEMENT" -> registerAdvancementListener(trigger, resetCommand);
                default -> plugin.getLogger().warning(
                        "[boosCooldowns] Unknown event trigger 'when': " + when);
            }
        }
        if (!registered.isEmpty()) {
            plugin.getLogger().info("[boosCooldowns] Registered "
                    + registered.size() + " event trigger(s).");
        }
    }

    public void unregisterAll() {
        for (Listener listener : registered) HandlerList.unregisterAll(listener);
        registered.clear();
    }

    private void registerKillListener(Map<?, ?> trigger, String resetCommand) {
        String entityName = String.valueOf(trigger.get("entity")).toUpperCase();
        EntityType type;
        try {
            type = EntityType.valueOf(entityName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[boosCooldowns] Unknown entity: " + entityName);
            return;
        }
        String message = stringOrNull(trigger.get("message"));
        String sound = stringOrNull(trigger.get("sound"));
        boolean broadcast = Boolean.TRUE.equals(trigger.get("broadcast"));

        Listener listener = new Listener() {};
        EventExecutor executor = (l, event) -> {
            if (!(event instanceof EntityDeathEvent death)) return;
            if (death.getEntity().getType() != type) return;
            Player killer = death.getEntity().getKiller();
            if (killer == null) return;
            cooldowns.removeCooldown(killer, resetCommand.toLowerCase());
            notify(killer, resetCommand, message, sound, broadcast,
                    "&entity&", type.name().toLowerCase().replace('_', ' '));
        };
        Bukkit.getPluginManager().registerEvent(
                EntityDeathEvent.class, listener, EventPriority.MONITOR, executor, plugin, true);
        registered.add(listener);
    }

    private void registerAdvancementListener(Map<?, ?> trigger, String resetCommand) {
        String advancementKey = String.valueOf(trigger.get("advancement"));
        String message = stringOrNull(trigger.get("message"));
        String sound = stringOrNull(trigger.get("sound"));
        boolean broadcast = Boolean.TRUE.equals(trigger.get("broadcast"));

        Listener listener = new Listener() {};
        EventExecutor executor = (l, event) -> {
            if (!(event instanceof PlayerAdvancementDoneEvent done)) return;
            if (!done.getAdvancement().getKey().getKey().equalsIgnoreCase(advancementKey)
                    && !done.getAdvancement().getKey().toString().equalsIgnoreCase(advancementKey)) return;
            cooldowns.removeCooldown(done.getPlayer(), resetCommand.toLowerCase());
            notify(done.getPlayer(), resetCommand, message, sound, broadcast,
                    "&advancement&", advancementKey);
        };
        Bukkit.getPluginManager().registerEvent(
                PlayerAdvancementDoneEvent.class, listener, EventPriority.MONITOR, executor, plugin, true);
        registered.add(listener);
    }

    private void notify(Player player, String resetCommand, String message,
                        String sound, boolean broadcast,
                        String extraPlaceholderKey, String extraPlaceholderValue) {
        if (message != null && !message.isEmpty()) {
            String rendered = message
                    .replace("&command&", resetCommand)
                    .replace("&player&", player.getName())
                    .replace(extraPlaceholderKey, extraPlaceholderValue);
            if (broadcast) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    BoosChat.sendMessage(online, rendered);
                }
            } else {
                BoosChat.sendMessage(player, rendered);
            }
        }
        if (sound != null && !sound.isEmpty()) {
            playSound(player, sound, broadcast);
        }
    }

    private void playSound(Player player, String soundName, boolean broadcast) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace('.', '_'));
            if (broadcast) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.playSound(online.getLocation(), sound, 1.0f, 1.0f);
                }
            } else {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[boosCooldowns] Unknown event_trigger sound: " + soundName);
        } catch (LinkageError e) {
            // future Paper builds may drop Sound.valueOf — fail silently
        }
    }

    private static String stringOrNull(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw);
        return s.isEmpty() || "null".equals(s) ? null : s;
    }
}
