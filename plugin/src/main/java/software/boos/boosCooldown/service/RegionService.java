package software.boos.boosCooldown.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Logger;

/**
 * Optional WorldGuard bridge. Class literals referencing WorldGuard types are
 * isolated in inner methods so plugin startup works without WorldGuard present
 * (NoClassDefFoundError is caught).
 */
public final class RegionService {

    private final boolean enabled;
    private final Logger logger;

    public RegionService(Logger logger) {
        this.logger = logger;
        boolean detected = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        this.enabled = detected && probe(logger);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Returns true if the player stands inside any of the given region IDs. */
    public boolean playerInAnyRegion(Player player, List<String> regionIds) {
        if (!enabled || regionIds == null || regionIds.isEmpty()) return true;
        try {
            return checkRegions(player, regionIds);
        } catch (LinkageError e) {
            return true;
        }
    }

    // Isolated call sites — only entered once after isPluginEnabled check.

    private static boolean probe(Logger logger) {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            logger.info("[boosCooldowns] WorldGuard integration enabled.");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static boolean checkRegions(Player player, List<String> regionIds) {
        com.sk89q.worldedit.bukkit.BukkitAdapter adapter;
        var container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
        var regionManager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(player.getWorld()));
        if (regionManager == null) return false;
        var location = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(player.getLocation());
        var applicable = regionManager.getApplicableRegions(location.toVector().toBlockPoint());
        for (var region : applicable.getRegions()) {
            if (regionIds.contains(region.getId())) return true;
        }
        return false;
    }
}
