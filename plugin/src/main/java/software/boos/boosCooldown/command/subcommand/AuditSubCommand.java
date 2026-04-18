package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.model.AuditEntry;
import software.boos.boosCooldown.service.AuditService;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.CommandSender;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Browse the audit log. Supports {@code /bcd audit} for the last 10 entries
 * and {@code /bcd audit <command> [limit]} for a per-command filter.
 */
public final class AuditSubCommand implements SubCommand {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final AuditService audit;

    public AuditSubCommand(AuditService audit) {
        this.audit = audit;
    }

    @Override public String name() { return "audit"; }
    @Override public String permission() { return "booscooldowns.audit"; }
    @Override public software.boos.boosCooldown.command.HelpCategory category() {
        return software.boos.boosCooldown.command.HelpCategory.DIAGNOSTICS;
    }
    @Override public String description() { return "Browse the command audit log"; }
    @Override public String usage() { return "[command] [limit]"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int limit = 10;
        List<AuditEntry> entries;
        if (args.length == 0) {
            entries = audit.recent(limit);
        } else {
            String cmd = args[0];
            if (args.length >= 2) {
                try {
                    limit = Math.min(100, Math.max(1, Integer.parseInt(args[1])));
                } catch (NumberFormatException ignored) {
                    // keep default
                }
            }
            entries = audit.forCommand(cmd, limit);
        }
        if (entries.isEmpty()) {
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e No audit entries.");
            return true;
        }
        BoosChat.sendMessage(sender, "&6=== Audit (" + entries.size() + " entries) ===");
        for (AuditEntry e : entries) {
            BoosChat.sendMessage(sender, "&7" + FORMAT.format(e.at()) + " &f"
                    + e.playerName() + " &7→ &f" + e.command() + " &7[&e" + e.outcome() + "&7]");
        }
        return true;
    }
}
