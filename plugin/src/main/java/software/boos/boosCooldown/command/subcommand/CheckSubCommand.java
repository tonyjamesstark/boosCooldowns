package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.HelpCategory;
import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.service.CommandDataFactory;
import software.boos.boosCooldown.service.CooldownService;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.TimeFormatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Replaces legacy {@code checkcooldown}. */
public final class CheckSubCommand implements SubCommand {

    private final PluginConfig config;
    private final MessageConfig messages;
    private final CommandDataFactory factory;
    private final CooldownService cooldowns;

    public CheckSubCommand(PluginConfig config, MessageConfig messages,
                           CommandDataFactory factory, CooldownService cooldowns) {
        this.config = config;
        this.messages = messages;
        this.factory = factory;
        this.cooldowns = cooldowns;
    }

    @Override public String name() { return "check"; }
    @Override public String permission() { return "booscooldowns.check.cooldown"; }
    @Override public HelpCategory category() { return HelpCategory.PLAYER; }
    @Override public String description() { return "Check whether a command is on cooldown for you"; }
    @Override public String usage() { return "<command>"; }
    @Override public List<String> aliases() { return List.of("checkcooldown"); }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 1) {
            BoosChat.sendMessage(sender, "&cUsage: /bcd check <command>");
            return true;
        }
        String cmd = args[0];
        String key = factory.resolveConfiguredKey(player, cmd);
        if (key == null) {
            BoosChat.sendMessage(player, messages.checkCooldownOk().replace("&command&", cmd));
            return true;
        }
        CommandData data = factory.build(player, cmd, key);
        if (!cooldowns.isOnCooldown(player, data)) {
            BoosChat.sendMessage(player, messages.checkCooldownOk().replace("&command&", cmd));
            return true;
        }
        long remaining = cooldowns.getRemainingCooldown(player, data);
        String formatted = TimeFormatter.formatRemaining(remaining,
                messages.unitHours(), messages.unitMinutes(), messages.unitSeconds());
        BoosChat.sendMessage(player, messages.checkCooldown()
                .replace("&command&", cmd)
                .replace("&seconds&", formatted)
                .replace("&unit&", ""));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) return List.of();
        String prefix = args[0].toLowerCase();
        List<String> out = new ArrayList<>();
        for (String cmd : config.getCommandsForPlayer(player)) {
            if (cmd.toLowerCase().startsWith(prefix)) out.add(cmd);
        }
        return out;
    }
}
