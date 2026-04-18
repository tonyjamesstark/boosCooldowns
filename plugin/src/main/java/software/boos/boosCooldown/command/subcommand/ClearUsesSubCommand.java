package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.LimitService;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.CommandKey;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** {@code /bcd clear uses <player> [command]}. Legacy alias: {@code /bcd clearuses}. */
public final class ClearUsesSubCommand implements SubCommand {

    private final LimitService limits;

    public ClearUsesSubCommand(LimitService limits) {
        this.limits = limits;
    }

    @Override public String name() { return "uses"; }
    @Override public String permission() { return "booscooldowns.clearuses"; }
    @Override public String description() { return "Reset usage counters for a player"; }
    @Override public String usage() { return "<player> [command]"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd clear uses <player> [command]");
            return true;
        }
        Player online = Bukkit.getPlayerExact(args[0]);
        if (online == null) {
            BoosChat.sendMessage(sender, "&cPlayer must be online: " + args[0]);
            return true;
        }
        if (args.length >= 2) {
            limits.resetUses(online, CommandKey.normalize(args[1]));
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e uses for " + args[1]
                    + " of " + args[0] + " cleared");
        } else {
            limits.resetUsesForPlayer(online);
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e uses of player "
                    + args[0] + " cleared");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) out.add(p.getName());
            }
            return out;
        }
        return List.of();
    }
}
