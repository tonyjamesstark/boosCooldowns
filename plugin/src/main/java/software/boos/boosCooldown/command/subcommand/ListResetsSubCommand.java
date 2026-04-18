package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.service.ScheduledResetService;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class ListResetsSubCommand implements SubCommand {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final ScheduledResetService service;

    public ListResetsSubCommand(ScheduledResetService service) {
        this.service = service;
    }

    @Override public String name() { return "list"; }
    @Override public String permission() { return "booscooldowns.scheduleglobalreset"; }
    @Override public String description() { return "Show pending scheduled resets"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Map<String, Instant> pending = service.pending();
        if (pending.isEmpty()) {
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e No pending scheduled resets.");
            return true;
        }
        BoosChat.sendMessage(sender, "&6[boosCooldowns]&e Pending scheduled resets:");
        for (Map.Entry<String, Instant> entry : pending.entrySet()) {
            BoosChat.sendMessage(sender, "&7 - &f" + entry.getKey()
                    + "&7 @ &e" + FORMAT.format(entry.getValue()));
        }
        return true;
    }
}
