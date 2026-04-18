package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.CommandSender;

public final class ReloadSubCommand implements SubCommand {

    private final BoosCoolDown plugin;

    public ReloadSubCommand(BoosCoolDown plugin) {
        this.plugin = plugin;
    }

    @Override public String name() { return "reload"; }
    @Override public String permission() { return "booscooldowns.reload"; }
    @Override public String description() { return "Reload config.yml and re-register listeners"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadPluginConfig();
        BoosChat.sendMessage(sender, "&6[boosCooldowns]&e config reloaded");
        return true;
    }
}
