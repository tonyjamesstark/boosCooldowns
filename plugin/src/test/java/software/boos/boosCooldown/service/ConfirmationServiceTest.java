package software.boos.boosCooldown.service;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.persistence.SqliteTestSupport;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.UserPreferencesRepository;
import software.boos.boosCooldown.persistence.repository.jdbc.JdbcUserPreferencesRepository;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationServiceTest {

    private ConfirmationService confirmations;
    private UserPreferencesRepository prefs;
    private PluginConfig config;
    private MessageConfig messages;
    private Player player;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        DataSource ds = SqliteTestSupport.createMigratedDataSource(tempDir);
        prefs = new JdbcUserPreferencesRepository(ds, StorageBackend.SQLITE);

        config = Mockito.mock(PluginConfig.class);
        messages = Mockito.mock(MessageConfig.class);
        Mockito.when(messages.confirmationToggleEnable()).thenReturn("enabled");
        Mockito.when(messages.confirmationToggleDisable()).thenReturn("disabled");
        BoosCoolDown plugin = Mockito.mock(BoosCoolDown.class);
        LimitService limits = Mockito.mock(LimitService.class);

        confirmations = new ConfirmationService(plugin, config, messages, limits, prefs, Runnable::run);

        player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void requiresConfirmationFollowsDefaultWhenUnset() {
        Mockito.when(config.isCommandConfirmationEnabled()).thenReturn(true);
        assertTrue(confirmations.requiresConfirmation(player));

        Mockito.when(config.isCommandConfirmationEnabled()).thenReturn(false);
        assertFalse(confirmations.requiresConfirmation(player));
    }

    @Test
    void toggleFlipsPerPlayerPreference() {
        Mockito.when(config.isCommandConfirmationEnabled()).thenReturn(true);
        // starts at default true → toggle → stored false
        confirmations.toggle(player);
        assertFalse(confirmations.requiresConfirmation(player));
        // toggle again → back to true
        confirmations.toggle(player);
        assertTrue(confirmations.requiresConfirmation(player));
    }

    @Test
    void perPlayerPreferenceOverridesGlobalDefault() {
        Mockito.when(config.isCommandConfirmationEnabled()).thenReturn(false);
        prefs.setBoolean(player.getUniqueId(), ConfirmationService.PREF_KEY, true);
        assertTrue(confirmations.requiresConfirmation(player));
    }
}
