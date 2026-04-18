package software.boos.boosCooldown.integration;

import software.boos.boosCooldown.BoosCoolDown;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link software.boos.boosCooldown.listener.WarmupCancelListener}.
 * Each flavour of cancel (move/sneak/sprint/damage/gamemode change) is validated
 * end-to-end by starting a real warmup and then firing the corresponding Bukkit
 * event.
 */
class WarmupCancelIntegrationTest {

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

    private void setOption(String path, Object value) {
        plugin.pluginConfig().raw().set(path, value);
        plugin.pluginConfig().save();
        plugin.reloadPluginConfig();
    }

    private PlayerMock addPlayer(String name) {
        PlayerMock player = server.addPlayer(name);
        player.setOp(false);
        return player;
    }

    private void startWarmup(PlayerMock player, String command) {
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, command);
        server.getPluginManager().callEvent(event);
        // drain warming-up message so later checks see only the cancel message
        while (player.nextMessage() != null) { /* drain */ }
    }

    @Test
    void moveCancelsWarmup() {
        configureCommand("/kit", "warmup", 5);
        setOption("options.options.cancel_warmup_on_move", true);

        PlayerMock player = addPlayer("Mover");
        startWarmup(player, "/kit");
        assertTrue(plugin.services().warmupService().hasWarmup(player),
                "Warmup should have started");

        Location from = player.getLocation();
        Location to = from.clone().add(1, 0, 1);
        PlayerMoveEvent move = new PlayerMoveEvent(player, from, to);
        server.getPluginManager().callEvent(move);

        assertFalse(plugin.services().warmupService().hasWarmup(player),
                "Move event should have cancelled the warmup");
    }

    @Test
    void sneakCancelsWarmup() {
        configureCommand("/kit", "warmup", 5);
        setOption("options.options.cancel_warmup_on_sneak", true);

        PlayerMock player = addPlayer("Sneaker");
        startWarmup(player, "/kit");
        assertTrue(plugin.services().warmupService().hasWarmup(player));

        PlayerToggleSneakEvent sneak = new PlayerToggleSneakEvent(player, true);
        server.getPluginManager().callEvent(sneak);

        assertFalse(plugin.services().warmupService().hasWarmup(player));
    }

    @Test
    void sprintCancelsWarmup() {
        configureCommand("/kit", "warmup", 5);
        setOption("options.options.cancel_warmup_on_sprint", true);

        PlayerMock player = addPlayer("Sprinter");
        startWarmup(player, "/kit");
        assertTrue(plugin.services().warmupService().hasWarmup(player));

        PlayerToggleSprintEvent sprint = new PlayerToggleSprintEvent(player, true);
        server.getPluginManager().callEvent(sprint);

        assertFalse(plugin.services().warmupService().hasWarmup(player));
    }

    @Test
    void damageCancelsWarmup() {
        configureCommand("/kit", "warmup", 5);
        setOption("options.options.cancel_warmup_on_damage", true);

        PlayerMock player = addPlayer("Victim");
        startWarmup(player, "/kit");
        assertTrue(plugin.services().warmupService().hasWarmup(player));

        EntityDamageEvent damage = new EntityDamageEvent(player,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.PLAYER_ATTACK).build(),
                1.0);
        server.getPluginManager().callEvent(damage);

        assertFalse(plugin.services().warmupService().hasWarmup(player));
    }

    @Test
    void gameModeChangeCancelsWarmup() {
        configureCommand("/kit", "warmup", 5);
        setOption("options.options.cancel_warmup_on_gamemode_change", true);

        PlayerMock player = addPlayer("Switcher");
        startWarmup(player, "/kit");
        assertTrue(plugin.services().warmupService().hasWarmup(player));

        PlayerGameModeChangeEvent change = new PlayerGameModeChangeEvent(player, GameMode.CREATIVE,
                PlayerGameModeChangeEvent.Cause.PLUGIN, null);
        server.getPluginManager().callEvent(change);

        assertFalse(plugin.services().warmupService().hasWarmup(player));
    }

    @Test
    void bypassPermissionKeepsWarmupOnMove() {
        configureCommand("/kit", "warmup", 5);
        setOption("options.options.cancel_warmup_on_move", true);

        PlayerMock player = addPlayer("Bypasser");
        player.addAttachment(plugin, "booscooldowns.nocancel.move", true);
        startWarmup(player, "/kit");
        assertTrue(plugin.services().warmupService().hasWarmup(player));

        Location from = player.getLocation();
        PlayerMoveEvent move = new PlayerMoveEvent(player, from, from.clone().add(1, 0, 0));
        server.getPluginManager().callEvent(move);

        assertTrue(plugin.services().warmupService().hasWarmup(player),
                "Player with nocancel.move should keep the warmup");
    }

    @Test
    void moveWithinSameBlockDoesNotCancel() {
        configureCommand("/kit", "warmup", 5);
        setOption("options.options.cancel_warmup_on_move", true);

        PlayerMock player = addPlayer("Twitcher");
        startWarmup(player, "/kit");

        Location from = player.getLocation();
        Location to = from.clone();
        to.setYaw(45); // rotation-only
        PlayerMoveEvent move = new PlayerMoveEvent(player, from, to);
        server.getPluginManager().callEvent(move);

        assertTrue(plugin.services().warmupService().hasWarmup(player),
                "Pure rotation should not cancel a warmup");
    }
}
