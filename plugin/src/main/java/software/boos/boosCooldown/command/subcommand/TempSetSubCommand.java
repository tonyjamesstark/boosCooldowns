package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.service.ConfigSyncService;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Temporarily override a command option. After the duration expires, the
 * original value is restored automatically.
 *
 * <pre>
 * /bcd tempset &lt;option&gt; &lt;command&gt; &lt;value&gt; &lt;duration&gt;
 * </pre>
 */
public final class TempSetSubCommand implements SubCommand {

    private final BoosCoolDown plugin;
    private final PluginConfig config;
    private final ConfigSyncService sync;

    public TempSetSubCommand(BoosCoolDown plugin, PluginConfig config, ConfigSyncService sync) {
        this.plugin = plugin;
        this.config = config;
        this.sync = sync;
    }

    @Override public String name() { return "tempset"; }
    @Override public String permission() { return "booscooldowns.set"; }
    @Override public String description() { return "Temporarily override an option and auto-restore"; }
    @Override public String usage() { return "<option> <command> <value> <duration>"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 4) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd tempset <option> <command> <value> <duration>");
            return true;
        }
        String option = args[0];
        String command = args[1].replace("_", " ");
        String value = args[2];
        long durationSeconds = TimeFormatter.parseSeconds(args[3]);
        if (durationSeconds <= 0) {
            BoosChat.sendMessage(sender, "&cDuration must be > 0: " + args[3]);
            return true;
        }

        var section = config.raw().getConfigurationSection("commands.groups.default");
        if (section == null) section = config.raw().createSection("commands.groups.default");
        var cmdSection = section.getConfigurationSection(command);
        if (cmdSection == null) cmdSection = section.createSection(command);

        final Object original = cmdSection.get(option);
        final var finalSection = cmdSection;

        // Apply the temporary override
        try {
            finalSection.set(option, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            finalSection.set(option, value);
        }
        config.save();
        plugin.reloadPluginConfig();

        BoosChat.sendMessage(sender, "&6[boosCooldowns]&e tempset " + option + " for "
                + command + " to " + value + " for " + durationSeconds + "s");

        if (sync != null && sync.isMultiNode()) {
            BoosChat.sendMessage(sender, "&e[boosCooldowns] Multi-node setup detected. "
                    + "This tempset only applies to " + sync.nodeId() + " — other nodes keep the old value.");
        }

        // Schedule the restore
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            var current = config.raw().getConfigurationSection("commands.groups.default");
            if (current == null) return;
            var cmd = current.getConfigurationSection(command);
            if (cmd == null) return;
            cmd.set(option, original);
            config.save();
            plugin.reloadPluginConfig();
            plugin.getLogger().info("[boosCooldowns] tempset expired, restored " + option
                    + " on " + command + " to " + original);
        }, durationSeconds * 20L);
        return true;
    }
}
