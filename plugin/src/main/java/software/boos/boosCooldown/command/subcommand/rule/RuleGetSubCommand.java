package software.boos.boosCooldown.command.subcommand.rule;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Prints the effective rule set for a command (after group inheritance). Useful
 * for admins to figure out why a command behaves a certain way without reading
 * config.yml manually.
 */
public final class RuleGetSubCommand implements SubCommand {

    private final PluginConfig config;

    public RuleGetSubCommand(PluginConfig config) {
        this.config = config;
    }

    @Override public String name() { return "get"; }
    @Override public String permission() { return "booscooldowns.rule.get"; }
    @Override public String description() { return "Print the effective rules for a command"; }
    @Override public String usage() { return "<command> [group]"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd rule get <command> [group]");
            return true;
        }
        String command = args[0].replace("_", " ");

        ConfigurationSection section;
        if (args.length >= 2) {
            section = config.raw().getConfigurationSection("commands.groups." + args[1] + "." + command);
            if (section == null) {
                BoosChat.sendMessage(sender, "&cNo rule for " + command + " in group " + args[1]);
                return true;
            }
        } else if (sender instanceof Player player) {
            section = config.commandSection(player, command);
            if (section == null) {
                BoosChat.sendMessage(sender, "&cNo rule applies to " + command + " for you");
                return true;
            }
        } else {
            BoosChat.sendMessage(sender, "&cConsole must specify a group: /bcd rule get <command> <group>");
            return true;
        }

        BoosChat.sendMessage(sender, "&6=== /bcd rule get " + command + " ===");
        for (String key : section.getKeys(true)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection) continue; // nested - we flatten leaves
            BoosChat.sendMessage(sender, "  &e" + key + "&7 = &f" + value);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            List<String> commands = new ArrayList<>();
            for (String cmd : config.getCommandsForPlayer(player)) {
                if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) commands.add(cmd);
            }
            return commands;
        }
        if (args.length == 2) {
            return new ArrayList<>(config.getGroups()).stream()
                    .filter(g -> g.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
