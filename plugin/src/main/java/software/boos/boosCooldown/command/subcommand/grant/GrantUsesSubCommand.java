package software.boos.boosCooldown.command.subcommand.grant;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.LimitService;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.CommandKey;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Set a player's remaining uses for a command. Primary use-case: reward /
 * punishment (give someone extra daily kit uses, reset a stuck counter).
 */
public final class GrantUsesSubCommand implements SubCommand {

    private final LimitService limits;

    public GrantUsesSubCommand(LimitService limits) {
        this.limits = limits;
    }

    @Override public String name() { return "uses"; }
    @Override public String permission() { return "booscooldowns.grant"; }
    @Override public String description() { return "Set remaining uses for a command on a player"; }
    @Override public String usage() { return "<player> <command> <count>"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd grant uses <player> <command> <count>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            BoosChat.sendMessage(sender, "&cPlayer must be online: " + args[0]);
            return true;
        }
        int count;
        try {
            count = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            BoosChat.sendMessage(sender, "&cCount must be an integer: " + args[2]);
            return true;
        }
        if (count < 0) {
            BoosChat.sendMessage(sender, "&cCount cannot be negative");
            return true;
        }
        String key = CommandKey.normalize(args[1]);
        limits.setUses(target, key, count);
        BoosChat.sendMessage(sender, "&6[boosCooldowns]&e set uses for " + args[1]
                + " on " + target.getName() + " to " + count);
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
