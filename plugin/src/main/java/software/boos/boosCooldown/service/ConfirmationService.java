package software.boos.boosCooldown.service;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.persistence.repository.UserPreferencesRepository;
import software.boos.boosCooldown.util.BoosChat;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Manages per-player confirmation flow for commands that have a price,
 * XP cost, item cost or limit. Stores pending actions in memory and toggles
 * user preferences in the {@link UserPreferencesRepository}.
 */
public final class ConfirmationService {

    public static final String PREF_KEY = "confirmations";
    private static final long PENDING_TTL_SECONDS = 30;

    private final BoosCoolDown plugin;
    private final PluginConfig config;
    private final MessageConfig messages;
    private final LimitService limits;
    private final UserPreferencesRepository preferences;
    private final Executor asyncExecutor;

    private final Map<UUID, PendingAction> pending = new ConcurrentHashMap<>();

    public ConfirmationService(BoosCoolDown plugin,
                               PluginConfig config,
                               MessageConfig messages,
                               LimitService limits,
                               UserPreferencesRepository preferences,
                               Executor asyncExecutor) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.limits = limits;
        this.preferences = preferences;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * Whether the given player requires a confirmation dialog. Falls back to
     * the global config flag when the player has no stored preference.
     */
    public boolean requiresConfirmation(Player player) {
        return preferences.getBoolean(player.getUniqueId(), PREF_KEY,
                config.isCommandConfirmationEnabled());
    }

    public void toggle(Player player) {
        boolean current = requiresConfirmation(player);
        preferences.setBoolean(player.getUniqueId(), PREF_KEY, !current);
        BoosChat.sendMessage(player, current
                ? messages.confirmationToggleDisable()
                : messages.confirmationToggleEnable());
    }

    /**
     * Prompts the player with a clickable Yes/No dialog and registers a
     * pending action that will execute on confirm.
     */
    public void request(Player player, CommandData data, Runnable onConfirm) {
        PendingAction previous = pending.remove(player.getUniqueId());
        if (previous != null) previous.expireTask.cancel();

        String token = UUID.randomUUID().toString().substring(0, 8);
        BukkitTask expire = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingAction removed = pending.remove(player.getUniqueId());
            if (removed != null && removed.token.equals(token)) {
                BoosChat.sendMessage(player, messages.commandCanceled()
                        .replace("&command&", data.originalCommand()));
            }
        }, PENDING_TTL_SECONDS * 20L);

        pending.put(player.getUniqueId(), new PendingAction(token, onConfirm, expire, data));

        sendDialog(player, data, token);
    }

    public void confirm(Player player, String token) {
        PendingAction action = pending.remove(player.getUniqueId());
        if (action == null || !action.token.equals(token)) {
            BoosChat.sendMessage(player, "&cNo pending confirmation found or token expired.");
            return;
        }
        action.expireTask.cancel();
        action.onConfirm.run();
    }

    public void cancel(Player player, String token) {
        PendingAction action = pending.remove(player.getUniqueId());
        if (action == null) {
            return;
        }
        if (!action.token.equals(token)) {
            pending.putIfAbsent(player.getUniqueId(), action);
            return;
        }
        action.expireTask.cancel();
        BoosChat.sendMessage(player, messages.commandCanceled()
                .replace("&command&", action.data.originalCommand()));
    }

    public void shutdown() {
        pending.values().forEach(p -> p.expireTask.cancel());
        pending.clear();
    }

    private void sendDialog(Player player, CommandData data, String token) {
        String question = BoosChat.translateColorCodes(
                messages.confirmationMessage().replace("&command&", data.originalCommand()));
        player.spigot().sendMessage(new TextComponent(question));

        // Summaries per price type
        if (data.hasMoneyPrice()) {
            String summary = messages.itsPrice()
                    .replace("&command&", data.originalCommand())
                    .replace("&price&", String.valueOf(data.moneyPrice()))
                    .replace("&balance&", "?");
            player.spigot().sendMessage(new TextComponent(BoosChat.translateColorCodes(summary)));
        }
        if (data.hasXpPrice()) {
            String summary = messages.itsXpPrice()
                    .replace("&command&", data.originalCommand())
                    .replace("&xpprice&", String.valueOf(data.xpPrice()));
            player.spigot().sendMessage(new TextComponent(BoosChat.translateColorCodes(summary)));
        }
        if (data.hasPlayerPointsPrice()) {
            String summary = messages.itsPlayerPointsPrice()
                    .replace("&command&", data.originalCommand())
                    .replace("&ppprice&", String.valueOf(data.playerPointsPrice()))
                    .replace("&ppbalance&", "?");
            player.spigot().sendMessage(new TextComponent(BoosChat.translateColorCodes(summary)));
        }
        if (data.hasItemCost()) {
            String summary = messages.itsItemCost()
                    .replace("&command&", data.originalCommand())
                    .replace("&itemprice&", String.valueOf(data.itemCost().count()))
                    .replace("&itemname&", data.itemCost().material());
            player.spigot().sendMessage(new TextComponent(BoosChat.translateColorCodes(summary)));
        }
        if (data.hasLimit()) {
            int remaining = limits.getRemainingUses(player, data);
            String summary = messages.itsLimit()
                    .replace("&command&", data.originalCommand())
                    .replace("&limit&", String.valueOf(data.limit()))
                    .replace("&uses&", String.valueOf(remaining));
            player.spigot().sendMessage(new TextComponent(BoosChat.translateColorCodes(summary)));
        }

        TextComponent yes = new TextComponent(BoosChat.translateColorCodes(
                "&a[" + messages.confirmationConfirm() + "]"));
        yes.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/booscooldowns confirm " + token));
        yes.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(messages.confirmationConfirmHint()).create()));

        TextComponent spacer = new TextComponent(" ");
        TextComponent no = new TextComponent(BoosChat.translateColorCodes(
                "&c[" + messages.confirmationCancel() + "]"));
        no.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/booscooldowns cancel " + token));
        no.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(messages.confirmationCancelHint()).create()));

        TextComponent wrapper = new TextComponent();
        wrapper.addExtra(yes);
        wrapper.addExtra(spacer);
        wrapper.addExtra(no);
        player.spigot().sendMessage(wrapper);
    }

    private record PendingAction(String token, Runnable onConfirm, BukkitTask expireTask, CommandData data) {
    }
}
