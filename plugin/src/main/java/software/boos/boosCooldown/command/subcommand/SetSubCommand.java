package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.service.ConfigSyncService;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.CommandSender;

public final class SetSubCommand implements SubCommand {

    private final BoosCoolDown plugin;
    private final PluginConfig config;
    private final ConfigSyncService sync;

    public SetSubCommand(BoosCoolDown plugin, PluginConfig config, ConfigSyncService sync) {
        this.plugin = plugin;
        this.config = config;
        this.sync = sync;
    }

    @Override public String name() { return "set"; }
    @Override public String permission() { return "booscooldowns.set"; }
    @Override public String description() { return "Persistently set a command option in config.yml"; }
    @Override public String usage() { return "<option> <command> <value> [group]"; }

    private static final java.util.List<String> KNOWN_OPTIONS = java.util.List.of(
            "cooldown", "warmup", "server_cooldown", "limit", "limit_reset_delay",
            "price", "xpcost", "xprequirement", "playerpoints",
            "disabled", "cancel_command", "message", "permission",
            "cooldown_message", "warmup_message", "disabled_message",
            "cooldown_multiplier", "cooldown_reset_after");

    @Override
    public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return KNOWN_OPTIONS.stream().filter(o -> o.startsWith(prefix)).toList();
        }
        if (args.length == 2 && sender instanceof org.bukkit.entity.Player p) {
            String prefix = args[1].toLowerCase();
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String cmd : config.getCommandsForPlayer(p)) {
                if (cmd.toLowerCase().startsWith(prefix)) out.add(cmd);
            }
            return out;
        }
        if (args.length == 4) {
            return new java.util.ArrayList<>(config.getGroups());
        }
        return java.util.List.of();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd set <what> <command> <value> [group]");
            return true;
        }
        String what = args[0];
        String command = args[1].replace("_", " ");
        String value = args[2];
        String group = args.length >= 4 ? args[3] : "default";
        if (!command.startsWith("/") && !command.equals("*")) {
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e Command has to start with \"/\".");
            return true;
        }
        config.setCommandOption(group, command, what, value);
        config.save();
        plugin.reloadPluginConfig();
        BoosChat.sendMessage(sender, "&6[boosCooldowns]&e " + what + " for command " + command
                + " in group " + group + " is now set to " + value);

        if (sync != null && sync.isMultiNode()) {
            BoosChat.sendMessage(sender, "&e[boosCooldowns] Multi-node setup detected. "
                    + "This change only applies to this server (" + sync.nodeId() + "). "
                    + "Edit config.yml on every other node and run /bcd reload there, "
                    + "or use /bcd diff to inspect the drift.");
        }
        return true;
    }
}
