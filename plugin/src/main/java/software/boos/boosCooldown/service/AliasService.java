package software.boos.boosCooldown.service;

import software.boos.boosCooldown.config.PluginConfig;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.regex.Pattern;

public final class AliasService {

    private final PluginConfig config;

    public AliasService(PluginConfig config) {
        this.config = config;
    }

    /**
     * Resolves an alias for a command. Supports wildcard patterns like "/ja *" → "/me $*",
     * "/who $player $world", etc. Returns null if no alias applies.
     */
    public String resolve(Player player, String rawCommand) {
        Set<String> aliases = config.getAliases();
        if (aliases.isEmpty()) return null;

        String lower = rawCommand.toLowerCase();
        // Exact match first
        for (String aliasKey : aliases) {
            if (aliasKey.equalsIgnoreCase(lower) && !aliasKey.contains("*")) {
                return substitute(config.getAlias(aliasKey), player, new String[0]);
            }
        }
        // Wildcard match
        for (String aliasKey : aliases) {
            if (!aliasKey.contains("*")) continue;
            String keyNormalized = aliasKey.toLowerCase();
            // Keep the space before '*': "/w *" must not match "/warp" (3.x matched "/w .+").
            String prefix = keyNormalized.substring(0, keyNormalized.indexOf('*'));
            if (lower.startsWith(prefix)) {
                String remainder = rawCommand.substring(prefix.length()).trim();
                String[] args = remainder.isEmpty() ? new String[0] : remainder.split("\\s+");
                return substitute(config.getAlias(aliasKey), player, args);
            }
        }
        return null;
    }

    private String substitute(String template, Player player, String[] args) {
        if (template == null) return null;
        String result = template;
        result = result.replace("$player", player.getName());
        result = result.replace("$world", player.getWorld().getName());
        result = result.replace("$*", String.join(" ", args));
        for (int i = 0; i < args.length; i++) {
            result = result.replace("$" + (i + 1), args[i]);
        }
        result = result.replaceAll("\\$\\d+", "");
        result = Pattern.compile("\\s+").matcher(result).replaceAll(" ").trim();
        return result;
    }
}
