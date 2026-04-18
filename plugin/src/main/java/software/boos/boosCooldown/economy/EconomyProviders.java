package software.boos.boosCooldown.economy;

import software.boos.boosCooldown.config.MessageConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Factory helpers that create economy providers without introducing direct
 * class references on paths that run before the dependency is verified to be
 * present. Each optional integration is isolated here so a missing plugin
 * only produces {@link NoClassDefFoundError} when its own factory method is
 * actually called — and that only happens when {@code isPluginEnabled} says
 * the jar is on the classpath.
 */
public final class EconomyProviders {

    private EconomyProviders() {
    }

    public static Optional<EconomyProvider> loadVault(MessageConfig messages, boolean configEnabled, Logger logger) {
        if (!configEnabled) return Optional.empty();
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            logger.info("[boosCooldowns] Vault not found, disabling economy support.");
            return Optional.empty();
        }
        try {
            return createVault(messages, logger);
        } catch (LinkageError e) {
            logger.warning("[boosCooldowns] Vault is present but Economy API could not be linked: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<EconomyProvider> loadPlayerPoints(MessageConfig messages, boolean configEnabled, Logger logger) {
        if (!configEnabled) return Optional.empty();
        if (!Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) return Optional.empty();
        try {
            return createPlayerPoints(messages, logger);
        } catch (LinkageError e) {
            logger.warning("[boosCooldowns] PlayerPoints linkage error: " + e.getMessage());
            return Optional.empty();
        }
    }

    // Isolated call sites: loading these methods triggers the class link for
    // the optional dependency. Callers above guard with isPluginEnabled first.

    private static Optional<EconomyProvider> createVault(MessageConfig messages, Logger logger) {
        RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (rsp == null) {
            logger.info("[boosCooldowns] Vault present but no Economy provider registered.");
            return Optional.empty();
        }
        net.milkbowl.vault.economy.Economy economy = rsp.getProvider();
        logger.info("[boosCooldowns] Found Vault economy: " + economy.getName());
        return Optional.of(new VaultEconomyProvider(economy, messages, true));
    }

    private static Optional<EconomyProvider> createPlayerPoints(MessageConfig messages, Logger logger) {
        Plugin raw = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (!(raw instanceof org.black_ixx.playerpoints.PlayerPoints pp)) {
            return Optional.empty();
        }
        logger.info("[boosCooldowns] PlayerPoints integration enabled.");
        return Optional.of(new PlayerPointsProvider(pp, messages, true));
    }
}
