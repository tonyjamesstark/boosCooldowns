package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.WarmupService;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** {@code /bcd clear warmups <player>}. Legacy alias: {@code /bcd clearwarmups}. */
public final class ClearWarmupsSubCommand implements SubCommand {

    private final WarmupService warmups;

    public ClearWarmupsSubCommand(WarmupService warmups) {
        this.warmups = warmups;
    }

    @Override public String name() { return "warmups"; }
    @Override public String permission() { return "booscooldowns.clearwarmups"; }
    @Override public String description() { return "Cancel all active warmups for a player"; }
    @Override public String usage() { return "<player>"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd clear warmups <player>");
            return true;
        }
        Player online = Bukkit.getPlayerExact(args[0]);
        if (online == null) {
            BoosChat.sendMessage(sender, "&cPlayer must be online: " + args[0]);
            return true;
        }
        warmups.cancelWarmups(online);
        BoosChat.sendMessage(sender, "&6[boosCooldowns]&e warmups of player " + args[0] + " cleared");
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
