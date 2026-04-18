package software.boos.boosCooldown.command.subcommand;

import software.boos.boosCooldown.command.SubCommand;
import software.boos.boosCooldown.model.ConfigVersion;
import software.boos.boosCooldown.service.ConfigSyncService;
import software.boos.boosCooldown.util.BoosChat;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Reports config-hash alignment across every node that has published a
 * version to the shared DB. Useful when the admin suspects two servers are
 * running with different {@code config.yml} contents.
 */
public final class DiffSubCommand implements SubCommand {

    private final ConfigSyncService sync;

    public DiffSubCommand(ConfigSyncService sync) {
        this.sync = sync;
    }

    @Override public String name() { return "diff"; }
    @Override public String permission() { return "booscooldowns.diff"; }
    @Override public software.boos.boosCooldown.command.HelpCategory category() {
        return software.boos.boosCooldown.command.HelpCategory.DIAGNOSTICS;
    }
    @Override public String description() { return "Compare this node's config hash with peers"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        List<ConfigVersion> nodes = sync.allNodes();
        if (nodes.isEmpty()) {
            BoosChat.sendMessage(sender, "&6[boosCooldowns]&e No nodes have registered yet.");
            return true;
        }
        String myHash = sync.currentHash();
        String myNode = sync.nodeId();

        long distinctHashes = nodes.stream().map(ConfigVersion::contentHash).distinct().count();
        if (distinctHashes == 1 && nodes.stream().allMatch(v -> v.contentHash().equals(myHash))) {
            BoosChat.sendMessage(sender, "&a[boosCooldowns] All "
                    + nodes.size() + " node(s) in sync. Hash: " + myHash.substring(0, 8));
            return true;
        }

        BoosChat.sendMessage(sender, "&c[boosCooldowns] Config drift detected across "
                + nodes.size() + " node(s):");
        Instant now = Instant.now();
        for (ConfigVersion v : nodes) {
            String marker = v.nodeId().equals(myNode) ? " &7(this)&r" : "";
            String color = v.contentHash().equals(myHash) ? "&a" : "&c";
            String ago = humanizeAgo(Duration.between(v.reloadedAt(), now));
            BoosChat.sendMessage(sender, "&7 - " + color + v.nodeId()
                    + " &7hash=" + v.contentHash().substring(0, 8)
                    + " &7reloaded &f" + ago + " ago" + marker);
        }
        BoosChat.sendMessage(sender, "&eEdit config.yml on every node to match, then run /bcd reload there.");
        return true;
    }

    private static String humanizeAgo(Duration d) {
        long secs = Math.max(0, d.toSeconds());
        if (secs < 60) return secs + "s";
        if (secs < 3600) return (secs / 60) + "m";
        if (secs < 86400) return (secs / 3600) + "h";
        return (secs / 86400) + "d";
    }
}
