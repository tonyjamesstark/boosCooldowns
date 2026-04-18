package software.boos.boosCooldown.command.subcommand.grant;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.service.CommandDataFactory;
import software.boos.boosCooldown.service.CooldownService;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.CommandKey;
import software.boos.boosCooldown.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class GrantCooldownSubCommand implements SubCommand {

    private final CommandDataFactory factory;
    private final CooldownService cooldowns;

    public GrantCooldownSubCommand(CommandDataFactory factory, CooldownService cooldowns) {
        this.factory = factory;
        this.cooldowns = cooldowns;
    }

    @Override public String name() { return "cooldown"; }
    @Override public String permission() { return "booscooldowns.grant"; }
    @Override public String description() { return "Manually seed a cooldown on a player"; }
    @Override public String usage() { return "<player> <command> <duration>"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd grant cooldown <player> <command> <duration>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            BoosChat.sendMessage(sender, "&cPlayer must be online: " + args[0]);
            return true;
        }
        String cmd = args[1];
        long seconds = TimeFormatter.parseSeconds(args[2]);
        if (seconds <= 0) {
            BoosChat.sendMessage(sender, "&cDuration must be > 0 seconds: " + args[2]);
            return true;
        }
        CommandData override = CommandData.builder()
                .originalCommand(cmd)
                .commandKey(CommandKey.normalize(cmd))
                .player(target)
                .cooldownSeconds(seconds)
                .build();
        cooldowns.setCooldown(target, override);
        BoosChat.sendMessage(sender, "&6[boosCooldowns]&e granted " + seconds + "s cooldown on "
                + cmd + " for " + target.getName());
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
