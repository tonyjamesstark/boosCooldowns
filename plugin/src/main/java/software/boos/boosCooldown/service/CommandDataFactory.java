package software.boos.boosCooldown.service;

import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.model.ItemCost;
import software.boos.boosCooldown.util.CommandKey;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.regex.Pattern;

public final class CommandDataFactory {

    private final PluginConfig config;

    public CommandDataFactory(PluginConfig config) {
        this.config = config;
    }

    /**
     * Resolves the configured regex/wildcard key that matches the given raw
     * command. Returns null if nothing in the group matches (i.e. command has
     * no configured cooldown/warmup/etc.).
     */
    public String resolveConfiguredKey(Player player, String rawCommand) {
        String normalized = CommandKey.normalize(rawCommand);
        Set<String> commands = config.getCommandsForPlayer(player);
        for (String configured : commands) {
            String regex = "^" + Pattern.quote(configured).replace("*", "\\E.*\\Q") + "$";
            if (configured.equalsIgnoreCase(normalized)
                    || configured.equalsIgnoreCase("*")
                    || normalized.matches(regex.replace("\\Q\\E", ""))) {
                return configured.toLowerCase();
            }
            if (configured.contains("*")) {
                String pattern = ("^" + configured.toLowerCase() + "$").replace("*", ".*");
                if (normalized.matches(pattern)) {
                    return configured.toLowerCase();
                }
            }
        }
        return null;
    }

    public CommandData build(Player player, String originalCommand, String configuredKey) {
        String lowerOriginal = originalCommand.toLowerCase();
        ItemCost itemCost = null;
        String material = config.getItemCostMaterial(player, configuredKey);
        if (material != null && !material.isEmpty()) {
            itemCost = new ItemCost(
                    material,
                    config.getItemCostCount(player, configuredKey),
                    config.getItemCostName(player, configuredKey),
                    config.getItemCostLore(player, configuredKey),
                    config.getItemCostEnchants(player, configuredKey));
        }
        return CommandData.builder()
                .originalCommand(lowerOriginal)
                .commandKey(configuredKey)
                .player(player)
                .cooldownSeconds(config.getCooldownSeconds(player, configuredKey))
                .warmupSeconds(config.getWarmupSeconds(player, configuredKey))
                .serverCooldownSeconds(config.getServerCooldownSeconds(player, configuredKey))
                .limit(config.getLimit(player, configuredKey))
                .limitResetDelaySeconds(config.getLimitResetDelaySeconds(player, configuredKey))
                .moneyPrice(config.getMoneyPrice(player, configuredKey))
                .xpPrice(config.getXpPrice(player, configuredKey))
                .xpRequirement(config.getXpRequirement(player, configuredKey))
                .playerPointsPrice(config.getPlayerPointsPrice(player, configuredKey))
                .itemCost(itemCost)
                .sharedCooldowns(config.getSharedCooldowns(player, configuredKey))
                .sharedLimits(config.getSharedLimits(player, configuredKey))
                .potionEffects(config.getPotionEffects(player, configuredKey))
                .disabled(config.isCommandDisabled(player, configuredKey))
                .cancelCommand(config.isCancelCommand(player, configuredKey))
                .redirectMessage(config.getCommandMessage(player, configuredKey))
                .requiredPermission(config.getRequiredPermission(player, configuredKey))
                .permissionDeniedMessage(config.getPermissionDeniedMessage(player, configuredKey))
                .allowedWorlds(config.getCommandStringList(player, configuredKey, "worlds"))
                .allowedRegions(config.getCommandStringList(player, configuredKey, "regions"))
                .warmupSound(config.getCommandString(player, configuredKey, "warmup_sound", null))
                .completeSound(config.getCommandString(player, configuredKey, "complete_sound", null))
                .warmupParticle(config.getCommandString(player, configuredKey, "warmup_particle", null))
                .cooldownMultiplier(config.getCommandDouble(player, configuredKey, "cooldown_multiplier", 1.0))
                .cooldownMultiplierResetSeconds(
                        software.boos.boosCooldown.util.TimeFormatter.parseSeconds(
                                config.getCommandString(player, configuredKey, "cooldown_reset_after", "0")))
                .build();
    }
}
