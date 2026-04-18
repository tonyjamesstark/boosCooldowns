package software.boos.boosCooldown.integration;

import software.boos.boosCooldown.BoosCoolDown;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end tests of the /booscooldowns subcommand router. Each test wires a
 * fresh plugin, runs the command via MockBukkit's dispatcher, and asserts on
 * the reply messages and side effects on the services.
 */
class SubCommandIntegrationTest {

    private ServerMock server;
    private BoosCoolDown plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(BoosCoolDown.class);
        plugin.pluginConfig().raw().set("options.options.command_confirmation", false);
        plugin.pluginConfig().save();
        plugin.reloadPluginConfig();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Non-op player — resolves to the 'default' group. */
    private PlayerMock addPlayer(String name) {
        PlayerMock player = server.addPlayer(name);
        player.setOp(false);
        return player;
    }

    /** Op player — needed for /bcd reload|set|clearcooldowns etc. */
    private PlayerMock addOp(String name) {
        PlayerMock player = server.addPlayer(name);
        player.setOp(true);
        return player;
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

    @Test
    void reloadRespondsWithConfirmation() {
        PlayerMock admin = addOp("Admin");
        admin.performCommand("booscooldowns reload");
        assertSaid(admin, msg -> msg.toLowerCase().contains("reloaded"));
    }

    @Test
    void limitsListsConfiguredLimitsForPlayer() {
        configureCommand("/kit", "limit", 3);

        PlayerMock player = addPlayer("Player");
        player.performCommand("booscooldowns limits");
        assertSaid(player, msg -> msg.contains("/kit") && msg.contains("3"));
    }

    @Test
    void checkCooldownReportsOkWhenNoActiveCooldown() {
        PlayerMock player = addPlayer("Checker");
        player.performCommand("booscooldowns checkcooldown /kit");
        assertSaid(player, msg -> msg.toLowerCase().contains("available"));
    }

    @Test
    void checkCooldownReportsRemainingWhenActive() {
        configureCommand("/kit", "cooldown", "60 seconds");

        PlayerMock player = addPlayer("User");

        // Trigger cooldown through a preprocess event
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/kit");
        server.getPluginManager().callEvent(event);
        sleep(150);
        while (player.nextMessage() != null) { /* drain */ }

        // Non-op player still has check.cooldown default:true permission
        player.performCommand("booscooldowns checkcooldown /kit");
        assertSaid(player, msg -> msg.toLowerCase().contains("still") || msg.contains("seconds"));
    }

    @Test
    void clearCooldownsWipesPlayerCooldowns() {
        configureCommand("/kit", "cooldown", "60 seconds");

        PlayerMock target = addPlayer("Target");
        // seed a cooldown
        server.getPluginManager().callEvent(new PlayerCommandPreprocessEvent(target, "/kit"));
        sleep(150);
        while (target.nextMessage() != null) { /* drain */ }

        PlayerMock admin = addOp("Admin");
        admin.performCommand("booscooldowns clearcooldowns Target");
        assertSaid(admin, msg -> msg.toLowerCase().contains("cleared"));
        sleep(150);

        // Target can now use the command again
        PlayerCommandPreprocessEvent reuse = new PlayerCommandPreprocessEvent(target, "/kit");
        server.getPluginManager().callEvent(reuse);
        assertFalse(reuse.isCancelled(), "After clearcooldowns, second use must pass");
    }

    @Test
    void unknownSubcommandReportsInvalid() {
        PlayerMock admin = addOp("A");
        admin.performCommand("booscooldowns bogus");
        assertSaid(admin, msg -> msg.toLowerCase().contains("unknown"));
    }

    @Test
    void setMutatesConfigAndReloads() {
        PlayerMock admin = addOp("Admin");
        admin.performCommand("booscooldowns set cooldown /warp 120");
        assertSaid(admin, msg -> msg.contains("120"));
        // Verify the mutation landed in the in-memory config
        ConfigurationSection groups = plugin.pluginConfig().raw()
                .getConfigurationSection("commands.groups.default");
        assertNotNull(groups);
        ConfigurationSection cmd = groups.getConfigurationSection("/warp");
        assertNotNull(cmd);
        assertTrue(cmd.getString("cooldown", "").contains("120"));
    }

    @Test
    void confirmationsToggleFlipsPreference() {
        PlayerMock player = addPlayer("Flipper");
        player.performCommand("booscooldowns confirmations");
        // should respond with either enable or disable text
        assertSaid(player, msg -> msg.toLowerCase().contains("confirmation"));
    }

    @Test
    void listResetsReportsEmptyWhenNoneScheduled() {
        PlayerMock admin = addOp("Admin");
        admin.performCommand("booscooldowns listresets");
        assertSaid(admin, msg -> msg.toLowerCase().contains("no pending"));
    }

    @Test
    void scheduleGlobalResetPersistsEntry() {
        PlayerMock admin = addOp("Admin");
        admin.performCommand("booscooldowns scheduleglobalreset /daily +1h");
        assertSaid(admin, msg -> msg.toLowerCase().contains("scheduled"));
        sleep(150);
        assertTrue(plugin.services().scheduledResetService().pending()
                .containsKey("/daily"), "Reset should be persisted for /daily");
    }

    private static void assertSaid(PlayerMock player, Predicate<String> matcher) {
        List<String> seen = new ArrayList<>();
        String message;
        while ((message = player.nextMessage()) != null) {
            seen.add(message);
            if (matcher.test(message)) return;
        }
        fail("No message matching predicate. Messages seen: " + seen);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
