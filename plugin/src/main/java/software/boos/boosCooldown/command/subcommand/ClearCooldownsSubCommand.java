package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.CooldownService;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.CommandKey;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** {@code /bcd clear cooldowns <player> [command]}. Legacy alias: {@code /bcd clearcooldowns}. */
public final class ClearCooldownsSubCommand implements SubCommand {

    private final CooldownService cooldowns;

    public ClearCooldownsSubCommand(CooldownService cooldowns) {
        this.cooldowns = cooldowns;
    }

    @Override public String name() { return "cooldowns"; }
    @Override public String permission() { return "booscooldowns.clearcooldowns"; }
    @Override public String description() { return "Clear active cooldowns for a player"; }
    @Override public String usage() { return "<player> [command]"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd clear cooldowns <player> [command]");
            return true;
        }
        Player online = Bukkit.getPlayerExact(args[0]);
        if (online == null) {
            BoosChat.sendMessage(sender, "&cPlayer must be online: " + args[0]);
            return true;
        }
        if (args.length >= 2) {
            cooldowns.removeCooldown(online, CommandKey.normalize(args[1]));
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e cooldown for "
                    + args[1] + " of " + args[0] + " cleared");
        } else {
            cooldowns.resetCooldowns(online);
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e cooldowns of player "
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
