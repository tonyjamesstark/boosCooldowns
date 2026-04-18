package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.HelpCategory;
import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.service.CommandDataFactory;
import software.boos.boosCooldown.service.CooldownService;
import software.boos.boosCooldown.service.LimitService;
import software.boos.boosCooldown.service.WarmupService;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Full plugin-state overview for a player — active cooldowns, remaining
 * uses, live warmups. Replaces legacy {@code info} (admin-targeted) and
 * {@code limits} (self-targeted); {@code /bcd status} with no argument
 * looks up the sender, with a name it inspects another player (requires
 * {@code booscooldowns.status.others}).
 */
public final class StatusSubCommand implements SubCommand {

    private final PluginConfig config;
    private final CommandDataFactory factory;
    private final CooldownService cooldowns;
    private final LimitService limits;
    private final WarmupService warmups;

    public StatusSubCommand(PluginConfig config, CommandDataFactory factory,
                            CooldownService cooldowns, LimitService limits, WarmupService warmups) {
        this.config = config;
        this.factory = factory;
        this.cooldowns = cooldowns;
        this.limits = limits;
        this.warmups = warmups;
    }

    @Override public String name() { return "status"; }
    @Override public String permission() { return "booscooldowns.status"; }
    @Override public HelpCategory category() { return HelpCategory.PLAYER; }
    @Override public String description() { return "Show your cooldowns, limits and warmups"; }
    @Override public String usage() { return "[player]"; }
    @Override public List<String> aliases() { return List.of("info", "limits"); }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player self)) {
                BoosChat.sendMessage(sender, "&cSpecify a player: /bcd status <player>");
                return true;
            }
            target = self;
        } else {
            if (!sender.hasPermission("booscooldowns.status.others")) {
                BoosChat.sendMessage(sender, "&cYou lack permission to inspect other players");
                return true;
            }
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                BoosChat.sendMessage(sender, "&cPlayer must be online: " + args[0]);
                return true;
            }
        }

        List<String> activeCooldowns = new ArrayList<>();
        List<String> limitLines = new ArrayList<>();
        for (String key : config.getCommandsForPlayer(target)) {
            CommandData data = factory.build(target, key, key);
            long remaining = cooldowns.getRemainingCooldown(target, data);
            if (remaining > 0) {
                activeCooldowns.add(key + " (" + TimeFormatter.formatRemaining(
                        remaining, "h", "m", "s") + ")");
            }
            if (data.hasLimit()) {
                int left = limits.getRemainingUses(target, data);
                limitLines.add(key + ": " + left + "/" + data.limit());
            }
        }

        BoosChat.sendMessage(sender, "&6=== boosCooldowns status — &e" + target.getName() + " &6===");
        BoosChat.sendMessage(sender, "&eGroup: &f" + config.resolveGroup(target));
        BoosChat.sendMessage(sender, "&eCooldowns: &f"
                + (activeCooldowns.isEmpty() ? "(none active)" : String.join(", ", activeCooldowns)));
        BoosChat.sendMessage(sender, "&eLimits: &f"
                + (limitLines.isEmpty() ? "(none)" : String.join(", ", limitLines)));
        BoosChat.sendMessage(sender, "&eWarmup: &f"
                + (warmups.hasWarmup(target) ? "active" : "none"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 1 || !sender.hasPermission("booscooldowns.status.others")) return List.of();
        List<String> names = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) names.add(p.getName());
        }
        return names;
    }
}
