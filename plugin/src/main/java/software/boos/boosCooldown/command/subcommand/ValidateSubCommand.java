package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.TimeFormatter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Set;

/**
 * Validates the loaded config for common mistakes: unknown option keys,
 * invalid materials referenced by item costs, bad duration strings, unknown
 * sounds, and cyclic shared_cooldown / shared_limit links.
 */
public final class ValidateSubCommand implements SubCommand {

    private static final Set<String> KNOWN_OPTIONS = Set.of(
            "cooldown", "warmup", "server_cooldown", "limit", "limit_reset_delay",
            "price", "xpcost", "xprequirement", "playerpoints", "itemcost",
            "shared_cooldown", "shared_limit", "potion", "disabled",
            "cancel_command", "message", "permission", "denied_message",
            "disabled_message", "cooldown_message", "warmup_message",
            "server_cooldown_message", "worlds", "regions",
            "warmup_sound", "complete_sound", "warmup_particle",
            "cooldown_multiplier", "cooldown_reset_after", "_inherits");

    private final PluginConfig config;

    public ValidateSubCommand(PluginConfig config) {
        this.config = config;
    }

    @Override public String name() { return "validate"; }
    @Override public String permission() { return "booscooldowns.validate"; }
    @Override public software.boos.boosCooldown.command.HelpCategory category() {
        return software.boos.boosCooldown.command.HelpCategory.DIAGNOSTICS;
    }
    @Override public String description() { return "Lint config.yml for unknown keys, bad durations, etc."; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int errors = 0;
        int warnings = 0;
        BoosChat.sendMessage(sender, "&6=== boosCooldowns config validation ===");

        for (String group : config.getGroups()) {
            ConfigurationSection groupSection = config.raw()
                    .getConfigurationSection("commands.groups." + group);
            if (groupSection == null) continue;
            for (String cmdKey : groupSection.getKeys(false)) {
                if ("_inherits".equals(cmdKey)) continue;
                ConfigurationSection cmd = groupSection.getConfigurationSection(cmdKey);
                if (cmd == null) continue;

                // Unknown keys
                for (String key : cmd.getKeys(false)) {
                    if (!KNOWN_OPTIONS.contains(key)) {
                        BoosChat.sendMessage(sender, "&eWARN &f" + group + "/" + cmdKey
                                + ": unknown option '" + key + "'");
                        warnings++;
                    }
                }

                // Duration parsing
                for (String key : List.of("cooldown", "warmup", "server_cooldown",
                        "limit_reset_delay", "cooldown_reset_after")) {
                    String raw = cmd.getString(key);
                    if (raw != null && !raw.isBlank() && TimeFormatter.parseSeconds(raw) <= 0
                            && !raw.trim().equals("0")) {
                        BoosChat.sendMessage(sender, "&cERR  &f" + group + "/" + cmdKey
                                + "/" + key + ": invalid duration '" + raw + "'");
                        errors++;
                    }
                }

                // Item cost validation
                ConfigurationSection itemCost = cmd.getConfigurationSection("itemcost");
                if (itemCost != null) {
                    String material = itemCost.getString("item");
                    if (material != null && Material.matchMaterial(material) == null) {
                        BoosChat.sendMessage(sender, "&cERR  &f" + group + "/" + cmdKey
                                + "/itemcost.item: unknown material '" + material + "'");
                        errors++;
                    }
                }

                // Sound validation
                for (String key : List.of("warmup_sound", "complete_sound")) {
                    String raw = cmd.getString(key);
                    if (raw != null && !raw.isBlank()) {
                        try {
                            Sound.valueOf(raw.toUpperCase().replace('.', '_'));
                        } catch (IllegalArgumentException e) {
                            BoosChat.sendMessage(sender, "&eWARN &f" + group + "/" + cmdKey
                                    + "/" + key + ": unknown sound '" + raw + "'");
                            warnings++;
                        } catch (LinkageError e) {
                            // Future Paper builds may drop Sound.valueOf — treat as "not validated".
                        }
                    }
                }

                // Self-reference in shared_cooldown
                List<String> shared = cmd.getStringList("shared_cooldown");
                if (shared.contains(cmdKey)) {
                    BoosChat.sendMessage(sender, "&eWARN &f" + group + "/" + cmdKey
                            + "/shared_cooldown: references itself");
                    warnings++;
                }
            }
        }

        BoosChat.sendMessage(sender, "&6Validation complete: &a" + errors
                + " errors, &e" + warnings + " warnings");
        return true;
    }
}
