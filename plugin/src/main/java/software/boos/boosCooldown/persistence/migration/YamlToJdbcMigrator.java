package software.boos.boosCooldown.persistence.migration;

import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.model.CooldownEntry;
import software.boos.boosCooldown.model.LimitEntry;
import software.boos.boosCooldown.persistence.repository.CooldownRepository;
import software.boos.boosCooldown.persistence.repository.LimitRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * One-shot migration of legacy users.yml data into the new JDBC repositories.
 * The old format used String.hashCode() as the key; we rebuild the mapping by
 * hashing every configured command key.
 */
public final class YamlToJdbcMigrator {

    private final File dataFolder;
    private final PluginConfig pluginConfig;
    private final CooldownRepository cooldownRepo;
    private final LimitRepository limitRepo;
    private final Logger logger;

    public YamlToJdbcMigrator(File dataFolder,
                              PluginConfig pluginConfig,
                              CooldownRepository cooldownRepo,
                              LimitRepository limitRepo,
                              Logger logger) {
        this.dataFolder = dataFolder;
        this.pluginConfig = pluginConfig;
        this.cooldownRepo = cooldownRepo;
        this.limitRepo = limitRepo;
        this.logger = logger;
    }

    public void migrateIfNeeded() {
        File usersFile = new File(dataFolder, "users.yml");
        if (!usersFile.exists() || usersFile.length() == 0) {
            return;
        }
        logger.info("[boosCooldowns] Legacy users.yml detected, migrating to DB...");
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(usersFile);
        Map<Integer, String> hashToKey = buildHashMap();

        ConfigurationSection users = legacy.getConfigurationSection("users");
        int migratedCooldowns = 0;
        int migratedLimits = 0;
        if (users != null) {
            SimpleDateFormat parser = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            for (String uuidStr : users.getKeys(false)) {
                UUID playerId;
                try {
                    playerId = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    logger.warning("[boosCooldowns] Skipping invalid UUID in users.yml: " + uuidStr);
                    continue;
                }
                ConfigurationSection cdSection = users.getConfigurationSection(uuidStr + ".cooldown");
                if (cdSection != null) {
                    for (String hashKey : cdSection.getKeys(false)) {
                        String command = hashToKey.get(tryParseInt(hashKey));
                        if (command == null) continue;
                        String timestamp = cdSection.getString(hashKey);
                        Instant expiresAt = parseLegacyDate(parser, timestamp);
                        if (expiresAt != null) {
                            cooldownRepo.upsert(new CooldownEntry(playerId, command, expiresAt));
                            migratedCooldowns++;
                        }
                    }
                }
                ConfigurationSection usesSection = users.getConfigurationSection(uuidStr + ".uses");
                if (usesSection != null) {
                    for (String hashKey : usesSection.getKeys(false)) {
                        String command = hashToKey.get(tryParseInt(hashKey));
                        if (command == null) continue;
                        int remaining = usesSection.getInt(hashKey, -1);
                        if (remaining >= 0) {
                            limitRepo.upsert(new LimitEntry(playerId, command, remaining, null));
                            migratedLimits++;
                        }
                    }
                }
            }
        }

        File renamed = new File(dataFolder,
                "users.yml.migrated-" + System.currentTimeMillis());
        if (usersFile.renameTo(renamed)) {
            logger.info("[boosCooldowns] Renamed users.yml to " + renamed.getName());
        } else {
            logger.warning("[boosCooldowns] Could not rename users.yml - migration will run again on next start");
        }
        logger.info("[boosCooldowns] Migration complete: "
                + migratedCooldowns + " cooldowns, " + migratedLimits + " limits");
    }

    private Map<Integer, String> buildHashMap() {
        Map<Integer, String> map = new HashMap<>();
        for (String group : pluginConfig.getGroups()) {
            for (String command : pluginConfig.getCommandsForGroup(group)) {
                String normalized = command.toLowerCase(Locale.ROOT);
                map.putIfAbsent(normalized.hashCode(), normalized);
            }
        }
        return map;
    }

    private static int tryParseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    private static Instant parseLegacyDate(SimpleDateFormat parser, String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return parser.parse(raw).toInstant();
        } catch (ParseException e) {
            return null;
        }
    }
}
