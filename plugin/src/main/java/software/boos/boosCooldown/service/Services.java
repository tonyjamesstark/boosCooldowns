package software.boos.boosCooldown.service;

import software.boos.boosCooldown.config.DatabaseConfig;
import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.config.PluginConfig;

/**
 * Lightweight DI container grouping every instance the plugin needs at
 * runtime. Created once in {@code BoosCoolDown.onEnable} and passed into
 * listeners and subcommands via constructor injection.
 */
public record Services(
        PluginConfig pluginConfig,
        MessageConfig messageConfig,
        DatabaseConfig databaseConfig,
        CooldownService cooldownService,
        WarmupService warmupService,
        WarmupServiceImpl warmupServiceImpl,
        LimitService limitService,
        PriceService priceService,
        AliasService aliasService,
        CommandDataFactory commandDataFactory,
        ActionBarService actionBarService,
        ScheduledResetService scheduledResetService,
        ConfirmationService confirmationService,
        WarmupEffectsService warmupEffectsService,
        RegionService regionService,
        AuditService auditService,
        ConfigSyncService configSyncService
) {
}
