package software.boos.boosCooldown.command.subcommand.clear;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.CooldownService;
import software.boos.boosCooldown.service.LimitService;
import software.boos.boosCooldown.service.WarmupService;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** {@code /bcd clear all <player>} — wipe cooldowns + uses + warmups in one call. */
public final class ClearAllSubCommand implements SubCommand {

    private final CooldownService cooldowns;
    private final LimitService limits;
    private final WarmupService warmups;

    public ClearAllSubCommand(CooldownService cooldowns, LimitService limits, WarmupService warmups) {
        this.cooldowns = cooldowns;
        this.limits = limits;
        this.warmups = warmups;
    }

    @Override public String name() { return "all"; }
    @Override public String permission() { return "booscooldowns.clear.all"; }
    @Override public String description() { return "Wipe cooldowns, limits and warmups for a player"; }
    @Override public String usage() { return "<player>"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd clear all <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            BoosChat.sendMessage(sender, "&cPlayer must be online: " + args[0]);
            return true;
        }
        cooldowns.resetCooldowns(target);
        limits.resetUsesForPlayer(target);
        warmups.cancelWarmups(target);
        BoosChat.sendMessage(sender, "&6[boosCooldowns]&e cleared cooldowns, uses and warmups for "
                + target.getName());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1) return List.of();
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) names.add(p.getName());
        }
        return names;
    }
}
