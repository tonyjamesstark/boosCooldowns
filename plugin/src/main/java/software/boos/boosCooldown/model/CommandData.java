package software.boos.boosCooldown.model;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of everything the pipeline needs about a command invocation.
 * Built once per command attempt by {@link software.boos.boosCooldown.service.CommandDataFactory}.
 */
public record CommandData(
        String originalCommand,
        String commandKey,
        Player player,
        long cooldownSeconds,
        long warmupSeconds,
        long serverCooldownSeconds,
        int limit,
        long limitResetDelaySeconds,
        double moneyPrice,
        int xpPrice,
        int xpRequirement,
        int playerPointsPrice,
        ItemCost itemCost,
        List<String> sharedCooldowns,
        List<String> sharedLimits,
        List<String> potionEffects,
        boolean disabled,
        boolean cancelCommand,
        String redirectMessage,
        String requiredPermission,
        String permissionDeniedMessage,
        java.util.List<String> allowedWorlds,
        java.util.List<String> allowedRegions,
        String warmupSound,
        String completeSound,
        String warmupParticle,
        double cooldownMultiplier,
        long cooldownMultiplierResetSeconds
) {

    public boolean hasCooldown() {
        return cooldownSeconds > 0;
    }

    public boolean hasWarmup() {
        return warmupSeconds > 0;
    }

    public boolean hasServerCooldown() {
        return serverCooldownSeconds > 0;
    }

    public boolean hasLimit() {
        return limit >= 0;
    }

    public boolean hasMoneyPrice() {
        return moneyPrice > 0.0;
    }

    public boolean hasXpPrice() {
        return xpPrice > 0;
    }

    public boolean hasXpRequirement() {
        return xpRequirement > 0;
    }

    public boolean hasPlayerPointsPrice() {
        return playerPointsPrice > 0;
    }

    public boolean hasItemCost() {
        return itemCost != null && itemCost.count() > 0;
    }

    public boolean hasPotionEffects() {
        return potionEffects != null && !potionEffects.isEmpty();
    }

    public boolean hasWorldRestriction() {
        return allowedWorlds != null && !allowedWorlds.isEmpty();
    }

    public boolean hasRegionRestriction() {
        return allowedRegions != null && !allowedRegions.isEmpty();
    }

    public boolean hasProgressiveCooldown() {
        return cooldownMultiplier > 1.0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String originalCommand = "";
        private String commandKey = "";
        private Player player;
        private long cooldownSeconds;
        private long warmupSeconds;
        private long serverCooldownSeconds;
        private int limit = -1;
        private long limitResetDelaySeconds;
        private double moneyPrice;
        private int xpPrice;
        private int xpRequirement;
        private int playerPointsPrice;
        private ItemCost itemCost;
        private List<String> sharedCooldowns = List.of();
        private List<String> sharedLimits = List.of();
        private List<String> potionEffects = List.of();
        private boolean disabled;
        private boolean cancelCommand;
        private String redirectMessage = "";
        private String requiredPermission;
        private String permissionDeniedMessage;
        private List<String> allowedWorlds = List.of();
        private List<String> allowedRegions = List.of();
        private String warmupSound;
        private String completeSound;
        private String warmupParticle;
        private double cooldownMultiplier = 1.0;
        private long cooldownMultiplierResetSeconds;

        public Builder originalCommand(String v) { this.originalCommand = v; return this; }
        public Builder commandKey(String v) { this.commandKey = v; return this; }
        public Builder player(Player v) { this.player = v; return this; }
        public Builder cooldownSeconds(long v) { this.cooldownSeconds = v; return this; }
        public Builder warmupSeconds(long v) { this.warmupSeconds = v; return this; }
        public Builder serverCooldownSeconds(long v) { this.serverCooldownSeconds = v; return this; }
        public Builder limit(int v) { this.limit = v; return this; }
        public Builder limitResetDelaySeconds(long v) { this.limitResetDelaySeconds = v; return this; }
        public Builder moneyPrice(double v) { this.moneyPrice = v; return this; }
        public Builder xpPrice(int v) { this.xpPrice = v; return this; }
        public Builder xpRequirement(int v) { this.xpRequirement = v; return this; }
        public Builder playerPointsPrice(int v) { this.playerPointsPrice = v; return this; }
        public Builder itemCost(ItemCost v) { this.itemCost = v; return this; }
        public Builder sharedCooldowns(List<String> v) { this.sharedCooldowns = v == null ? List.of() : v; return this; }
        public Builder sharedLimits(List<String> v) { this.sharedLimits = v == null ? List.of() : v; return this; }
        public Builder potionEffects(List<String> v) { this.potionEffects = v == null ? List.of() : v; return this; }
        public Builder disabled(boolean v) { this.disabled = v; return this; }
        public Builder cancelCommand(boolean v) { this.cancelCommand = v; return this; }
        public Builder redirectMessage(String v) { this.redirectMessage = v == null ? "" : v; return this; }
        public Builder requiredPermission(String v) { this.requiredPermission = v; return this; }
        public Builder permissionDeniedMessage(String v) { this.permissionDeniedMessage = v; return this; }
        public Builder allowedWorlds(List<String> v) { this.allowedWorlds = v == null ? List.of() : v; return this; }
        public Builder allowedRegions(List<String> v) { this.allowedRegions = v == null ? List.of() : v; return this; }
        public Builder warmupSound(String v) { this.warmupSound = v; return this; }
        public Builder completeSound(String v) { this.completeSound = v; return this; }
        public Builder warmupParticle(String v) { this.warmupParticle = v; return this; }
        public Builder cooldownMultiplier(double v) { this.cooldownMultiplier = v; return this; }
        public Builder cooldownMultiplierResetSeconds(long v) { this.cooldownMultiplierResetSeconds = v; return this; }

        public CommandData build() {
            Objects.requireNonNull(originalCommand, "originalCommand");
            Objects.requireNonNull(commandKey, "commandKey");
            Objects.requireNonNull(player, "player");
            return new CommandData(originalCommand, commandKey, player,
                    cooldownSeconds, warmupSeconds, serverCooldownSeconds,
                    limit, limitResetDelaySeconds,
                    moneyPrice, xpPrice, xpRequirement, playerPointsPrice,
                    itemCost, sharedCooldowns, sharedLimits, potionEffects,
                    disabled, cancelCommand, redirectMessage,
                    requiredPermission, permissionDeniedMessage,
                    allowedWorlds, allowedRegions, warmupSound, completeSound,
                    warmupParticle, cooldownMultiplier, cooldownMultiplierResetSeconds);
        }
    }
}
