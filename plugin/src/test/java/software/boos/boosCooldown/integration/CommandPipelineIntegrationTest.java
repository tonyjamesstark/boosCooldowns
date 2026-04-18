package software.boos.boosCooldown.integration;

import software.boos.boosCooldown.BoosCoolDown;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the {@link software.boos.boosCooldown.listener.CommandPreprocessListener}
 * pipeline. Rather than relying on MockBukkit's chat simulation (which does
 * not always fire {@code PlayerCommandPreprocessEvent}) we construct and call
 * the event directly — this is exactly what Spigot does internally when a
 * real player types a command.
 */
class CommandPipelineIntegrationTest {

    private ServerMock server;
    private BoosCoolDown plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(BoosCoolDown.class);
        // Disable confirmations globally — tests that want to exercise the
        // confirmation flow override this explicitly. See ConfirmationIntegrationTest.
        plugin.pluginConfig().raw().set("options.options.command_confirmation", false);
        plugin.pluginConfig().save();
        plugin.reloadPluginConfig();
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
        // Default MockBukkit players are ops — which triggers disabled_for_ops.
        player.setOp(false);
        return player;
    }

    private PlayerCommandPreprocessEvent fireCommand(PlayerMock player, String raw) {
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, raw);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    void unconfiguredCommandFlowsThrough() {
        PlayerMock player = addPlayer("Alice");
        PlayerCommandPreprocessEvent event = fireCommand(player, "/uncategorized");
        assertFalse(event.isCancelled(), "Pipeline should ignore unconfigured commands");
        assertNull(player.nextMessage(), "No plugin message expected for unconfigured command");
    }

    @Test
    void configuredCooldownBlocksSecondInvocation() {
        configureCommand("/kit", "cooldown", "60 seconds");
        PlayerMock player = addPlayer("Bob");

        PlayerCommandPreprocessEvent first = fireCommand(player, "/kit");
        assertFalse(first.isCancelled(), "First use must pass through");
        waitForAsync();
        while (player.nextMessage() != null) { /* drain */ }

        PlayerCommandPreprocessEvent second = fireCommand(player, "/kit");
        assertTrue(second.isCancelled(), "Second use must be cancelled by cooldown");
        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.toLowerCase().contains("wait") || message.contains("seconds"),
                "Cooldown message should mention remaining seconds, got: " + message);
    }

    @Test
    void disabledCommandIsBlockedWithDisabledMessage() {
        configureCommand("/tpa", "disabled", true);
        PlayerMock player = addPlayer("Carol");

        PlayerCommandPreprocessEvent event = fireCommand(player, "/tpa");
        assertTrue(event.isCancelled(), "Disabled command must be cancelled");
        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.toLowerCase().contains("disabled"),
                "Disabled message expected, got: " + message);
    }

    @Test
    void nodisablePermissionBypassesDisabled() {
        configureCommand("/tpa", "disabled", true);
        PlayerMock player = addPlayer("Dan");
        player.addAttachment(plugin, "booscooldowns.nodisable", true);

        PlayerCommandPreprocessEvent event = fireCommand(player, "/tpa");
        assertFalse(event.isCancelled(), "nodisable permission must let the command through");
    }

    @Test
    void limitBlocksAfterConsumption() {
        configureCommand("/daily", "limit", 1);
        PlayerMock player = addPlayer("Eve");

        PlayerCommandPreprocessEvent first = fireCommand(player, "/daily");
        assertFalse(first.isCancelled());
        waitForAsync();
        while (player.nextMessage() != null) { /* drain */ }

        PlayerCommandPreprocessEvent second = fireCommand(player, "/daily");
        assertTrue(second.isCancelled(), "Second use must be blocked by limit");
        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.toLowerCase().contains("cannot") || message.toLowerCase().contains("anymore"),
                "Limit-exhausted message expected, got: " + message);
    }

    @Test
    void exceptionPermissionBypassesPipeline() {
        configureCommand("/kit", "cooldown", "60 seconds");
        PlayerMock player = addPlayer("Frank");
        player.addAttachment(plugin, "booscooldowns.exception", true);

        fireCommand(player, "/kit");
        waitForAsync();
        while (player.nextMessage() != null) { /* drain */ }

        PlayerCommandPreprocessEvent event = fireCommand(player, "/kit");
        assertFalse(event.isCancelled(), "Exception permission should bypass the plugin entirely");
        assertNull(player.nextMessage());
    }

    @Test
    void perCommandPermissionDeniesWithCustomMessage() {
        configureCommand("/vip", "permission", "myplugin.vip");
        configureCommand("/vip", "denied_message", "&cNot a VIP");
        PlayerMock player = addPlayer("Grace");

        PlayerCommandPreprocessEvent event = fireCommand(player, "/vip");
        assertTrue(event.isCancelled(), "Missing per-command permission must cancel");
        String message = player.nextMessage();
        assertNotNull(message);
        assertTrue(message.toLowerCase().contains("vip"),
                "Denied message expected, got: " + message);
    }

    @Test
    void namespacedCommandGetsRewrittenAndTriggersCooldown() {
        // Admin disables the outright block but keeps rewrite on.
        plugin.pluginConfig().raw().set("options.options.syntax_blocker_enabled", false);
        plugin.pluginConfig().raw().set("options.options.apply_rules_to_prefixed_syntax", true);
        plugin.pluginConfig().save();
        plugin.reloadPluginConfig();

        configureCommand("/kit", "cooldown", "60 seconds");
        PlayerMock player = addPlayer("Namespace");

        // First use via the namespaced form → passes, seeds the cooldown as /kit
        PlayerCommandPreprocessEvent first = fireCommand(player, "/minecraft:kit");
        assertFalse(first.isCancelled());
        assertEquals("/kit", first.getMessage(),
                "Pipeline must rewrite the event message to the bare form");
        waitForAsync();
        while (player.nextMessage() != null) { /* drain */ }

        // Second use via the bare form within the window → blocked.
        PlayerCommandPreprocessEvent second = fireCommand(player, "/kit");
        assertTrue(second.isCancelled(), "Cooldown must apply across namespace forms");
    }

    @Test
    void syntaxBlockerStillWinsWhenEnabled() {
        plugin.pluginConfig().raw().set("options.options.syntax_blocker_enabled", true);
        plugin.pluginConfig().save();
        plugin.reloadPluginConfig();

        PlayerMock player = addPlayer("Strict");
        player.setOp(false);
        PlayerCommandPreprocessEvent event = fireCommand(player, "/minecraft:tp");
        assertTrue(event.isCancelled(), "Blocker must reject namespaced commands outright");
    }

    @Test
    void serverCooldownBlocksAllPlayers() {
        configureCommand("/event", "server_cooldown", "10 minutes");
        PlayerMock a = addPlayer("PlayerA");
        PlayerMock b = addPlayer("PlayerB");

        PlayerCommandPreprocessEvent first = fireCommand(a, "/event");
        assertFalse(first.isCancelled());
        waitForAsync();

        PlayerCommandPreprocessEvent second = fireCommand(b, "/event");
        assertTrue(second.isCancelled(), "Second player must hit the server cooldown");
        String message = b.nextMessage();
        assertNotNull(message);
        assertTrue(message.toLowerCase().contains("server") || message.contains("seconds"),
                "Server cooldown message expected, got: " + message);
    }

    private void waitForAsync() {
        try {
            Thread.sleep(80);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
