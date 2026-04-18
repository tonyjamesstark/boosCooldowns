package software.boos.boosCooldown.command.subcommand.reset;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.LimitService;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.CommandKey;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Immediately wipe the usage counter for a command across all players
 * (replaces legacy {@code startglobalreset}).
 */
public final class ResetNowSubCommand implements SubCommand {

    private final LimitService limits;

    public ResetNowSubCommand(LimitService limits) {
        this.limits = limits;
    }

    @Override public String name() { return "now"; }
    @Override public String permission() { return "booscooldowns.scheduleglobalreset"; }
    @Override public String description() { return "Reset the global use counter for a command immediately"; }
    @Override public String usage() { return "<command>"; }
    @Override public List<String> aliases() { return List.of("startglobalreset"); }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd reset now <command>");
            return true;
        }
        String key = CommandKey.normalize(args[0]);
        limits.resetUsesForCommand(key);
        BoosChat.sendMessage(sender, "&6[boosCooldowns]&e cleared uses for " + args[0] + " globally");
        return true;
    }
}
