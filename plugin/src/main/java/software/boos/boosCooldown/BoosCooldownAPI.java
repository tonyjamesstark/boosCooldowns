package software.boos.boosCooldown;

import software.boos.boosCooldown.model.CommandData;
import software.boos.boosCooldown.service.Services;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class BoosCooldownAPI {

    private static BoosCooldownAPI instance;

    private final Services services;

    private BoosCooldownAPI(Services services) {
        this.services = services;
    }

    public static synchronized void initialize(Services services) {
        instance = new BoosCooldownAPI(Objects.requireNonNull(services));
    }

    public static synchronized void shutdown() {
        instance = null;
    }

    public static BoosCooldownAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BoosCooldownAPI not initialized - plugin not enabled yet");
        }
        return instance;
    }

    public CommandData buildCommandData(Player player, String rawCommand) {
        String configuredKey = services.commandDataFactory().resolveConfiguredKey(player, rawCommand);
        if (configuredKey == null) return null;
        return services.commandDataFactory().build(player, rawCommand, configuredKey);
    }

    public boolean isOnCooldown(Player player, CommandData data) {
        return services.cooldownService().isOnCooldown(player, data);
    }

    public long getRemainingCooldown(Player player, CommandData data) {
        return services.cooldownService().getRemainingCooldown(player, data);
    }

    public CompletableFuture<Void> setCooldown(Player player, CommandData data) {
        return services.cooldownService().setCooldown(player, data);
    }

    public CompletableFuture<Void> removeCooldown(Player player, CommandData data) {
        return services.cooldownService().removeCooldown(player, data);
    }

    public CompletableFuture<Void> resetCooldowns(Player player) {
        return services.cooldownService().resetCooldowns(player);
    }

    public boolean isServerOnCooldown(CommandData data) {
        return services.cooldownService().isServerOnCooldown(data);
    }

    public boolean hasWarmup(Player player) {
        return services.warmupService().hasWarmup(player);
    }

    public CompletableFuture<Void> cancelWarmups(Player player) {
        return services.warmupService().cancelWarmups(player);
    }

    public int getRemainingUses(Player player, CommandData data) {
        return services.limitService().getRemainingUses(player, data);
    }
}
