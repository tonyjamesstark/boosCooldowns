package software.boos.boosCooldown.config;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class MessageConfig {

    private final PluginConfig config;
    private LocaleMessages locales;

    public MessageConfig(PluginConfig config) {
        this.config = config;
    }

    public void setLocaleMessages(LocaleMessages locales) {
        this.locales = locales;
    }

    /** Expand &-color, per-player PlaceholderAPI placeholders, and locale fallback. */
    public String expand(Player player, String raw) {
        if (raw == null) return null;
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, raw);
            } catch (LinkageError ignored) {
                // PlaceholderAPI classes not available — pass through untouched
            }
        }
        return raw;
    }

    private Locale localeFor(Player player) {
        if (player == null) return config.getDefaultLocale();
        try {
            return player.locale();
        } catch (Throwable t) {
            return config.getDefaultLocale();
        }
    }

    private String lookup(Player player, String path, String fallback) {
        if (locales != null) return locales.get(localeFor(player), path, fallback);
        return config.getString(path, fallback);
    }

    // -------- per-command override helpers --------

    private String resolve(Player player, String commandKey, String perCommandField, String globalPath, String fallback) {
        if (player != null && commandKey != null) {
            String override = config.getCommandString(player, commandKey, perCommandField, null);
            if (override != null && !override.isEmpty()) {
                return override;
            }
        }
        return lookup(player, globalPath, fallback);
    }

    // -------- cooldown / warmup --------

    public String coolingDown(Player player, String commandKey) {
        return resolve(player, commandKey, "cooldown_message",
                "options.messages.cooling_down",
                "&6Wait&e &seconds& seconds&6 before you can use command&e &command& &6again.&f");
    }

    public String warmingUp(Player player, String commandKey) {
        return resolve(player, commandKey, "warmup_message",
                "options.messages.warming_up",
                "&6Wait&e &seconds& seconds&6 before command&e &command& &6has warmed up.&f");
    }

    public String warmupAlreadyStarted() {
        return config.getString("options.messages.warmup_already_started",
                "&6Warm-Up process for&e &command& &6has already started.&f");
    }

    public String warmupCancelledByDamage() {
        return config.getString("options.messages.warmup_cancelled_by_damage",
                "&6Warm-ups have been cancelled due to receiving damage.&f");
    }

    public String warmupCancelledByMove() {
        return config.getString("options.messages.warmup_cancelled_by_move",
                "&6Warm-ups have been cancelled due to moving.&f");
    }

    public String warmupCancelledBySneak() {
        return config.getString("options.messages.warmup_cancelled_by_sneak",
                "&6Warm-ups have been cancelled due to sneaking.&f");
    }

    public String warmupCancelledBySprint() {
        return config.getString("options.messages.warmup_cancelled_by_sprint",
                "&6Warm-ups have been cancelled due to sprinting.&f");
    }

    public String warmupCancelledByGameModeChange() {
        return config.getString("options.messages.warmup_cancelled_by_gamemode_change",
                "&6Warm-ups have been cancelled due to changing gamemode.&f");
    }

    public String interactBlocked() {
        return config.getString("options.messages.interact_blocked_during_warmup",
                "&6You can't do this when command is warming-up!&f");
    }

    // -------- cooldown checks --------

    public String checkCooldown() {
        return config.getString("options.messages.check_cooldown",
                "&6Command&e &command& &6is still on cooldown. It will be available again in&e &seconds& &unit&.&f");
    }

    public String checkCooldownOk() {
        return config.getString("options.messages.check_cooldown_ok",
                "&6Command&e &command& &6is available.");
    }

    // -------- server cooldown (new) --------

    public String serverCoolingDown(Player player, String commandKey) {
        return resolve(player, commandKey, "server_cooldown_message",
                "options.messages.server_cooling_down",
                "&6Server-wide cooldown for&e &command& &6is active. Wait &e&seconds&&6.&f");
    }

    // -------- disabled (new) --------

    public String disabled(Player player, String commandKey) {
        return resolve(player, commandKey, "disabled_message",
                "options.messages.disabled_command",
                "&cCommand &e&command& &cis currently disabled.&f");
    }

    // -------- limits --------

    public String limitAchieved() {
        return config.getString("options.messages.limit_achieved",
                "&6You cannot use this command anymore!&f");
    }

    public String limitList() {
        return config.getString("options.messages.limit_list",
                "&6Limit for command &e&command&&6 is &e&limit&&6. You can still use it &e&times&&6 times.&f");
    }

    public String limitReset() {
        return config.getString("options.messages.limit_reset",
                "&6Wait&e &seconds& &unit&&6 before your limit for command&e &command& &6is reset.&f");
    }

    public String limitResetNow() {
        return config.getString("options.messages.limit_reset_now",
                "&6Reseting limits for command&e &command& &6now.&f");
    }

    // -------- prices --------

    public String insufficientFunds() {
        return config.getString("options.messages.insufficient_funds",
                "&6You have insufficient funds!&e &command& &6costs &e%s &6but you only have &e%s");
    }

    public String paidForCommand() {
        return config.getString("options.messages.paid_for_command",
                "Price of &command& was %s and you now have %s");
    }

    public String insufficientItems() {
        return config.getString("options.messages.insufficient_items",
                "&6You have not enough items!&e &command& &6needs");
    }

    public String paidItemsForCommand() {
        return config.getString("options.messages.paid_items_for_command",
                "&6Price of&e &command& &6was &e%s");
    }

    public String insufficientXp() {
        return config.getString("options.messages.insufficient_xp",
                "&6You have not enough XP!&e &command& &6needs &e%s");
    }

    public String insufficientXpRequirement() {
        return config.getString("options.messages.insufficient_xp_requirement",
                "&6Your level is too low to use this!&e &command& &6needs &e%s");
    }

    public String paidXpForCommand() {
        return config.getString("options.messages.paid_xp_for_command",
                "&6Price of&e &command& &6was &e%s");
    }

    public String insufficientPlayerPoints() {
        return config.getString("options.messages.insufficient_player_points",
                "&6You have not enough PlayerPoints!&e &command& &6needs &e%s");
    }

    public String paidPlayerPointsForCommand() {
        return config.getString("options.messages.paid_player_points_for_command",
                "Price of &command& was %s PlayerPoints and you now have %s PlayerPoints");
    }

    // -------- units --------

    public String unitHours() {
        return config.getString("options.units.hours", "hours");
    }

    public String unitMinutes() {
        return config.getString("options.units.minutes", "minutes");
    }

    public String unitSeconds() {
        return config.getString("options.units.seconds", "seconds");
    }

    // -------- confirmations --------

    public String confirmationMessage() {
        return config.getString("options.messages.confirmation_message",
                "&6Would you like to use command&e &command& &6?");
    }

    public String confirmationConfirm() {
        return config.getString("options.messages.confirmation_confirm_command_execution", "Yes");
    }

    public String confirmationCancel() {
        return config.getString("options.messages.confirmation_cancel_command_execution", "No");
    }

    public String confirmationConfirmHint() {
        return config.getString("options.messages.confirmation_confirm_command_execution_hint", "Click to confirm");
    }

    public String confirmationCancelHint() {
        return config.getString("options.messages.confirmation_cancel_command_execution_hint", "Click to cancel");
    }

    public String confirmationToggleEnable() {
        return config.getString("options.messages.confirmation_toggle_enable",
                "Confirmation messages are now enabled for you!");
    }

    public String confirmationToggleDisable() {
        return config.getString("options.messages.confirmation_toggle_disable",
                "Confirmation messages are now disabled for you!");
    }

    public String itsPrice() {
        return config.getString("options.messages.confirmation_price_of_command",
                "&6its price is&e &price& &6and you now have &e&balance&");
    }

    public String itsItemCost() {
        return config.getString("options.messages.confirmation_item_price_of_command",
                "&6its price is&e &itemprice& &itemname&");
    }

    public String itsLimit() {
        return config.getString("options.messages.confirmation_limit_of_command",
                "&6it is limited to&e &limit& &6uses and you can still use it&e &uses& &6times");
    }

    public String itsXpPrice() {
        return config.getString("options.messages.confirmation_xp_price_of_command",
                "&6its price is&e &xpprice& experience levels");
    }

    public String itsPlayerPointsPrice() {
        return config.getString("options.messages.confirmation_player_points_price_of_command",
                "&6its price is&e &ppprice& PlayerPoints &6and you now have &e&ppbalance& PlayerPoints");
    }

    public String commandCanceled() {
        return config.getString("options.messages.confirmation_command_cancelled",
                "&6Execution of command&e &command& &6was cancelled");
    }

    // -------- signs / misc --------

    public String cannotCreateSign() {
        return config.getString("options.messages.cannot_create_sign",
                "&6You are not allowed to create this kind of signs!&f");
    }

    public String cannotUseSign() {
        return config.getString("options.messages.cannot_use_sign",
                "&6You are not allowed to use this sign!&f");
    }

    public String invalidCommandSyntax() {
        return config.getString("options.messages.invalid_command_syntax",
                "&6You are not allowed to use command syntax /<pluginname>:<command>!");
    }

    public String paidError() {
        return config.getString("options.messages.paid_error", "An error has occured: %s");
    }
}
