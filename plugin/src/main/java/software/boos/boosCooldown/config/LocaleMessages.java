package software.boos.boosCooldown.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Per-locale message overrides. When the plugin ships a
 * {@code messages_<locale>.yml} resource (or the admin drops one into the
 * data folder), keys present there override the values from the main
 * {@code config.yml}.
 *
 * <p>Locale is chosen per invocation: player locale → plugin default →
 * global {@code config.yml}. Keys are the same paths used in
 * {@link MessageConfig} (e.g. {@code options.messages.cooling_down}).
 */
public final class LocaleMessages {

    private final JavaPlugin plugin;
    private final PluginConfig fallback;
    private final Map<String, YamlConfiguration> loaded = new ConcurrentHashMap<>();

    public LocaleMessages(JavaPlugin plugin, PluginConfig fallback) {
        this.plugin = plugin;
        this.fallback = fallback;
    }

    /** Loads known locale files. Called on enable and on reload. */
    public void reload() {
        loaded.clear();
        for (String lang : new String[]{"en", "cs", "de", "es", "fr", "pl", "ru"}) {
            YamlConfiguration yaml = loadForLanguage(lang);
            if (yaml != null) loaded.put(lang, yaml);
        }
    }

    /**
     * Returns the message for {@code path}, resolved against the given
     * locale. Falls back to the plugin's default locale, then to the global
     * {@code config.yml} value.
     */
    public String get(Locale locale, String path, String def) {
        String lang = locale != null ? locale.getLanguage().toLowerCase() : "";
        YamlConfiguration messages = loaded.get(lang);
        if (messages != null) {
            String value = messages.getString(path);
            if (value != null) return value;
        }
        Locale fallbackLocale = fallback.getDefaultLocale();
        if (fallbackLocale != null && !fallbackLocale.getLanguage().equalsIgnoreCase(lang)) {
            YamlConfiguration fbMessages = loaded.get(fallbackLocale.getLanguage().toLowerCase());
            if (fbMessages != null) {
                String value = fbMessages.getString(path);
                if (value != null) return value;
            }
        }
        return fallback.getString(path, def);
    }

    private YamlConfiguration loadForLanguage(String lang) {
        File external = new File(plugin.getDataFolder(), "messages_" + lang + ".yml");
        if (external.isFile()) {
            return YamlConfiguration.loadConfiguration(external);
        }
        // Optional bundled resource
        try (InputStream in = plugin.getResource("messages_" + lang + ".yml")) {
            if (in == null) return null;
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "[boosCooldowns] Failed to read bundled messages_" + lang + ".yml", e);
            return null;
        }
    }
}
