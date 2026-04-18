package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.ScheduledResetService;
import software.boos.boosCooldown.util.BoosChat;
import software.boos.boosCooldown.util.CommandKey;
import software.boos.boosCooldown.util.TimeFormatter;
import org.bukkit.command.CommandSender;

import java.time.Instant;

public final class ScheduleGlobalResetSubCommand implements SubCommand {

    private final ScheduledResetService service;

    public ScheduleGlobalResetSubCommand(ScheduledResetService service) {
        this.service = service;
    }

    @Override public String name() { return "schedule"; }
    @Override public String permission() { return "booscooldowns.scheduleglobalreset"; }
    @Override public String description() { return "Schedule a global usage reset"; }
    @Override public String usage() { return "<command> <yyyy-MM-dd HH:mm:ss | +2h>"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            BoosChat.sendMessage(sender,
                    "&cUsage: /bcd reset schedule <command> <yyyy-MM-dd HH:mm:ss | +2h>");
            return true;
        }
        String key = CommandKey.normalize(args[0]);
        String timestamp = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        try {
            long epoch = TimeFormatter.parseFutureTimestampMillis(timestamp);
            service.schedule(key, Instant.ofEpochMilli(epoch));
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e global reset for " + key
                    + " scheduled at " + timestamp);
        } catch (IllegalArgumentException e) {
            BoosChat.sendMessage(sender, "&c" + e.getMessage());
        }
        return true;
    }
}
