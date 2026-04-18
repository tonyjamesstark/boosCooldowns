package software.boos.boosCooldown.command;

import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class CommandHandler implements CommandExecutor, TabCompleter {

    /** Routing table — primary names + aliases, both resolve to the same instance. */
    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();

    /**
     * Primary names in registration order. Aliases registered via
     * {@link #registerAlias(String, SubCommand)} are <i>not</i> added here, so
     * {@link #visibleCommands()} returns every subcommand exactly once.
     */
    private final Set<String> primaryKeys = new LinkedHashSet<>();

    public CommandHandler register(SubCommand subCommand) {
        String key = subCommand.name().toLowerCase();
        subcommands.put(key, subCommand);
        primaryKeys.add(key);
        for (String alias : subCommand.aliases()) {
            subcommands.putIfAbsent(alias.toLowerCase(), subCommand);
        }
        return this;
    }

    /**
     * Registers a subcommand under an additional top-level name. Use for
     * backward-compatibility aliases — {@code /bcd clearcooldowns} still
     * routes to the instance that now lives at {@code /bcd clear cooldowns}.
     * The alias does not appear in {@code /bcd help}.
     */
    public CommandHandler registerAlias(String aliasName, SubCommand target) {
        subcommands.putIfAbsent(aliasName.toLowerCase(), target);
        return this;
    }

    /** Returns the subcommand keyed by {@code name} or alias, or {@code null}. */
    public SubCommand find(String name) {
        return subcommands.get(name.toLowerCase());
    }

    /** Primary subcommands only — aliases filtered out. */
    public List<SubCommand> visibleCommands() {
        return primaryKeys.stream()
                .map(subcommands::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            SubCommand help = subcommands.get("help");
            if (help != null) return help.execute(sender, new String[0]);
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e Type /bcd help for a list of commands.");
            return true;
        }
        SubCommand sub = subcommands.get(args[0].toLowerCase());
        if (sub == null) {
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e Unknown subcommand: " + args[0]
                    + ". Type /bcd help.");
            return true;
        }
        String perm = sub.permission();
        if (perm != null && !perm.isEmpty() && !sender.hasPermission(perm)) {
            BoosChat.sendMessage(sender, "&cYou lack permission " + perm);
            return true;
        }
        String[] tail = args.length == 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
        return sub.execute(sender, tail);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            List<String> names = new ArrayList<>();
            for (SubCommand sub : visibleCommands()) {
                String perm = sub.permission();
                if (perm == null || perm.isEmpty() || sender.hasPermission(perm)) {
                    names.add(sub.name());
                }
            }
            return filterPrefix(names, args.length == 0 ? "" : args[0]);
        }
        SubCommand sub = subcommands.get(args[0].toLowerCase());
        if (sub == null) return Collections.emptyList();
        return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private static List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) result.add(opt);
        }
        return result;
    }
}
