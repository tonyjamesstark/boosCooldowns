package software.boos.boosCooldown.integration;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.service.ConfirmationService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for the {@link ConfirmationService} flow, including the
 * preference toggle and per-player override of the global default.
 */
class ConfirmationFlowIntegrationTest {

    private ServerMock server;
    private BoosCoolDown plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(BoosCoolDown.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private void configureCommand(String command, String option, Object value) {
        ConfigurationSection root = plugin.pluginConfig().raw();
        ConfigurationSection groups = root.getConfigurationSection("commands.groups.default");
        if (groups == null) groups = root.createSection("commands.groups.default");
        ConfigurationSection cmd = groups.getConfigurationSection(command);
        if (cmd == null) cmd = groups.createSection(command);
        cmd.set(option, value);
        plugin.pluginConfig().save();
        plugin.reloadPluginConfig();
    }

    private PlayerMock addPlayer(String name) {
        PlayerMock player = server.addPlayer(name);
        player.setOp(false);
        return player;
    }

    @Test
    void limitedCommandTriggersConfirmationDialog() {
        configureCommand("/shop", "limit", 1);

        PlayerMock player = addPlayer("Shopper");

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/shop");
        server.getPluginManager().callEvent(event);

        assertTrue(event.isCancelled(),
                "Command with price/limit must be captured for confirmation");
        // Confirmation dialog sends a "Would you like…" message + clickable Yes/No
        assertTrue(drainMessages(player).stream().anyMatch(
                msg -> msg.toLowerCase().contains("would you like") || msg.contains("?")),
                "A confirmation question should have been sent");
    }

    @Test
    void confirmationsOffSkipsDialog() {
        configureCommand("/shop", "limit", 1);

        PlayerMock player = addPlayer("NoDialog");
        // Toggle confirmations off for this player
        player.performCommand("booscooldowns confirmations");
        drainMessages(player);

        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/shop");
        server.getPluginManager().callEvent(event);

        // With confirmations disabled for this player, command proceeds directly
        assertFalse(event.isCancelled(),
                "Player with confirmations off should bypass the dialog");
    }

    @Test
    void togglePreferencePersistsAcrossReload() {
        PlayerMock player = addPlayer("Toggler");

        // Initially should match global default (true)
        assertTrue(plugin.services().confirmationService().requiresConfirmation(player));

        player.performCommand("booscooldowns confirmations");
        drainMessages(player);
        assertFalse(plugin.services().confirmationService().requiresConfirmation(player),
                "After toggle, pref should be false");

        // Reload plugin config — preference is in DB, survives reload
        plugin.reloadPluginConfig();
        assertFalse(plugin.services().confirmationService().requiresConfirmation(player),
                "Preference must survive config reload (it lives in the DB)");
    }

    @Test
    void confirmWithBogusTokenRejectsSilently() {
        PlayerMock player = addPlayer("BadToken");
        player.performCommand("booscooldowns confirm deadbeef");
        boolean sawError = drainMessages(player).stream()
                .anyMatch(msg -> msg.toLowerCase().contains("no pending"));
        assertTrue(sawError, "Bogus token should produce a 'no pending' error");
    }

    private static java.util.List<String> drainMessages(PlayerMock player) {
        java.util.List<String> result = new java.util.ArrayList<>();
        String m;
        while ((m = player.nextMessage()) != null) result.add(m);
        return result;
    }
}
