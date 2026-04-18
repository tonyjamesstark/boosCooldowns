package software.boos.boosCooldown.command;

import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A {@link SubCommand} that routes to further nested subcommands. Used for
 * {@code /bcd clear ...}, {@code /bcd rule ...}, {@code /bcd grant ...},
 * {@code /bcd reset ...}.
 */
public final class CommandGroup implements SubCommand {

    private final String name;
    private final String description;
    private final HelpCategory category;
    private final Map<String, SubCommand> children = new LinkedHashMap<>();
    private final List<String> aliases = new ArrayList<>();

    public CommandGroup(String name, String description, HelpCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public CommandGroup add(SubCommand child) {
        children.put(child.name().toLowerCase(), child);
        for (String alias : child.aliases()) {
            children.putIfAbsent(alias.toLowerCase(), child);
        }
        return this;
    }

    public CommandGroup addAlias(String alias) {
        aliases.add(alias);
        return this;
    }

    public Map<String, SubCommand> children() {
        return children;
    }

    @Override public String name() { return name; }
    @Override public List<String> aliases() { return aliases; }
    @Override public String description() { return description; }
    @Override public HelpCategory category() { return category; }

    @Override
    public String permission() {
        // A group itself doesn't require its own permission — each child gates
        // its own access. Returning empty lets the outer handler skip the check
        // and delegate to the child.
        return "";
    }

    @Override
    public String usage() {
        return "<" + children.values().stream()
                .map(SubCommand::name)
                .distinct()
                .collect(Collectors.joining("|")) + "> ...";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e /bcd " + name + " " + usage());
            return true;
        }
        SubCommand child = children.get(args[0].toLowerCase());
        if (child == null) {
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e Unknown: " + name + " " + args[0]);
            return true;
        }
        String perm = child.permission();
        if (perm != null && !perm.isEmpty() && !sender.hasPermission(perm)) {
            BoosChat.sendMessage(sender, "&cYou lack permission " + perm);
            return true;
        }
        return child.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            List<String> names = new ArrayList<>();
            for (SubCommand child : distinctChildren()) {
                if (hasPerm(sender, child)) names.add(child.name());
            }
            return filterPrefix(names, args.length == 0 ? "" : args[0]);
        }
        SubCommand child = children.get(args[0].toLowerCase());
        if (child == null) return Collections.emptyList();
        return child.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private List<SubCommand> distinctChildren() {
        return new ArrayList<>(new LinkedHashMap<>(children).values().stream()
                .distinct().collect(Collectors.toList()));
    }

    private static boolean hasPerm(CommandSender sender, SubCommand cmd) {
        String perm = cmd.permission();
        return perm == null || perm.isEmpty() || sender.hasPermission(perm);
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
