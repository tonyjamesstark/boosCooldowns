package software.boos.boosCooldown.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginConfigTest {

    private static final String OLD_CONFIG = """
            options:
              options:
                #should warmups be enabled?
                warmups_enabled: false
                disabled_for_ops: true
              units:
                seconds: sec
            commands:
              groups:
                default:
                  /home:
                    cooldown: 30
              aliases:
                /h: /home
            """;

    @TempDir
    Path dataFolder;

    private JavaPlugin plugin;
    private Path configFile;
    private Path backupFile;

    @BeforeEach
    void setUp() {
        plugin = Mockito.mock(JavaPlugin.class);
        Mockito.when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        Mockito.when(plugin.getLogger()).thenReturn(Logger.getLogger("PluginConfigTest"));
        Mockito.when(plugin.getResource("config.yml")).thenAnswer(inv -> bundledConfig());
        configFile = dataFolder.resolve("config.yml");
        backupFile = dataFolder.resolve("config.yml.bak");
    }

    private static InputStream bundledConfig() {
        return PluginConfigTest.class.getClassLoader().getResourceAsStream("config.yml");
    }

    @Test
    void addsMissingPluginOptionsWithoutTouchingUserSections() throws IOException {
        Files.writeString(configFile, OLD_CONFIG, StandardCharsets.UTF_8);
        byte[] original = Files.readAllBytes(configFile);

        new PluginConfig(plugin, "config.yml");
        YamlConfiguration saved = YamlConfiguration.loadConfiguration(configFile.toFile());

        assertEquals("sqlite", saved.getString("database.type"));
        assertEquals(4, saved.getInt("database.pool-size"));
        assertEquals(30, saved.getInt("database.cache-ttl-seconds"));
        assertTrue(saved.isBoolean("options.options.debug"));
        assertFalse(saved.getBoolean("options.options.debug"));
        assertTrue(saved.isBoolean("options.action_bar.warmup"));
        assertEquals(10, saved.getInt("options.action_bar.update_interval_ticks"));
        assertEquals(List.of("should debug logging be enabled?"), saved.getComments("options.options.debug"));
        assertEquals(List.of("how long the in-memory cache trusts DB values before refreshing"),
                saved.getComments("database.cache-ttl-seconds"));

        assertFalse(saved.getBoolean("options.options.warmups_enabled"));
        assertEquals("sec", saved.getString("options.units.seconds"));

        assertEquals(Set.of("/h"), saved.getConfigurationSection("commands.aliases").getKeys(false));
        assertEquals(Set.of("default"), saved.getConfigurationSection("commands.groups").getKeys(false));
        assertEquals(Set.of("/home"), saved.getConfigurationSection("commands.groups.default").getKeys(false));
        assertFalse(saved.contains("global"));

        assertArrayEquals(original, Files.readAllBytes(backupFile));

        byte[] upgraded = Files.readAllBytes(configFile);
        Files.delete(backupFile);
        new PluginConfig(plugin, "config.yml");
        assertArrayEquals(upgraded, Files.readAllBytes(configFile));
        assertFalse(Files.exists(backupFile), "second load must not rewrite the backup");
    }

    @Test
    void completeConfigIsNotWritten() throws IOException {
        try (InputStream in = bundledConfig()) {
            Files.copy(in, configFile);
        }
        byte[] original = Files.readAllBytes(configFile);

        new PluginConfig(plugin, "config.yml");

        assertArrayEquals(original, Files.readAllBytes(configFile));
        assertFalse(Files.exists(backupFile));
    }
}
