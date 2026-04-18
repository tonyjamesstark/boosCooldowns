package software.boos.boosCooldown.integration;

import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.BoosCooldownAPI;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginLifecycleIntegrationTest {

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

    @Test
    void pluginLoadsAndRegistersMainCommand() {
        assertTrue(plugin.isEnabled());
        assertNotNull(plugin.getCommand("booscooldowns"),
                "Main /booscooldowns command must be registered");
        PluginDescriptionFile desc = plugin.getDescription();
        assertEquals("boosCooldowns", desc.getName());
    }

    @Test
    void servicesContainerIsInitialized() {
        assertNotNull(plugin.services(), "Services container must be populated after onEnable");
        assertNotNull(plugin.services().cooldownService());
        assertNotNull(plugin.services().warmupService());
        assertNotNull(plugin.services().limitService());
        assertNotNull(plugin.services().priceService());
    }

    @Test
    void apiIsAccessibleAfterEnable() {
        assertNotNull(BoosCooldownAPI.getInstance());
    }

    @Test
    void apiIsShutdownAfterDisable() {
        plugin.onDisable();
        assertThrows(IllegalStateException.class, BoosCooldownAPI::getInstance);
    }
}
