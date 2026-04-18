package software.boos.boosCooldown.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public interface SubCommand {

    String name();

    String permission();

    boolean execute(CommandSender sender, String[] args);

    /**
     * Alternative names used for backward compatibility with pre-4.0 command
     * layout (e.g. {@code clearcooldowns} now maps to {@code clear cooldowns}).
     * Aliases are routable but hidden from {@code /bcd help}.
     */
    default List<String> aliases() {
        return Collections.emptyList();
    }

    /** One-line description shown by {@code /bcd help}. */
    default String description() {
        return "";
    }

    /** Argument pattern shown by {@code /bcd help} — e.g. "&lt;player&gt; [command]". */
    default String usage() {
        return "";
    }

    /** Help grouping. Defaults to {@link HelpCategory#ADMIN}. */
    default HelpCategory category() {
        return HelpCategory.ADMIN;
    }

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
