package software.boos.boosCooldown.listener;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.boos.boosCooldown.BoosCoolDown;
import software.boos.boosCooldown.config.MessageConfig;
import software.boos.boosCooldown.config.PluginConfig;
import software.boos.boosCooldown.service.ActionBarService;
import software.boos.boosCooldown.service.AliasService;
import software.boos.boosCooldown.service.CommandDataFactory;
import software.boos.boosCooldown.service.ConfirmationService;
import software.boos.boosCooldown.service.CooldownService;
import software.boos.boosCooldown.service.LimitService;
import software.boos.boosCooldown.service.PriceService;
import software.boos.boosCooldown.service.RegionService;
import software.boos.boosCooldown.service.WarmupEffectsService;
import software.boos.boosCooldown.service.WarmupService;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandPreprocessListenerUnitTest {

    @Test
    void stripsStandardNamespace() {
        assertEquals("/tp spawn",
                CommandPreprocessListener.stripNamespacePrefix("/minecraft:tp spawn"));
    }

    @Test
    void stripsNamespaceOnCommandWithoutArgs() {
        assertEquals("/list",
                CommandPreprocessListener.stripNamespacePrefix("/bukkit:list"));
    }

    @Test
    void leavesPlainCommandAlone() {
        assertEquals("/tp spawn",
                CommandPreprocessListener.stripNamespacePrefix("/tp spawn"));
    }

    @Test
    void leavesColonInArgumentsAlone() {
        // The colon is after a space — part of an argument, not a namespace.
        assertEquals("/me time is 12:34",
                CommandPreprocessListener.stripNamespacePrefix("/me time is 12:34"));
    }

    @Test
    void rejectsColonAtPositionZero() {
        assertEquals("/:weird",
                CommandPreprocessListener.stripNamespacePrefix("/:weird"));
    }

    @Test
    void acceptsUnderscoresAndDotsInNamespace() {
        assertEquals("/tp",
                CommandPreprocessListener.stripNamespacePrefix("/my_plugin.core:tp"));
    }

    @Test
    void rejectsInvalidNamespaceCharacters() {
        // Space inside the prefix means it's not a valid Bukkit plugin namespace.
        assertEquals("/foo bar:baz",
                CommandPreprocessListener.stripNamespacePrefix("/foo bar:baz"));
    }

    @Test
    void leavesNullAndEmptyAlone() {
        assertEquals(null, CommandPreprocessListener.stripNamespacePrefix(null));
        assertEquals("", CommandPreprocessListener.stripNamespacePrefix(""));
        assertEquals("/", CommandPreprocessListener.stripNamespacePrefix("/"));
    }

    /** 3.x applied aliases before the exception/OP check, so OPs and exempt players get them too. */
    @Test
    void aliasesApplyToPlayersThePluginIsOffFor() {
        PluginConfig config = Mockito.mock(PluginConfig.class);
        Mockito.when(config.isDisabledForOps()).thenReturn(true);
        Mockito.when(config.getAliases()).thenReturn(Set.of("/scut"));
        Mockito.when(config.getAlias("/scut")).thenReturn("/stonecutter");
        CommandPreprocessListener listener = new CommandPreprocessListener(
                Mockito.mock(BoosCoolDown.class), config, Mockito.mock(MessageConfig.class),
                Mockito.mock(CommandDataFactory.class), new AliasService(config),
                Mockito.mock(CooldownService.class), Mockito.mock(WarmupService.class),
                Mockito.mock(LimitService.class), Mockito.mock(PriceService.class),
                Mockito.mock(ActionBarService.class), Mockito.mock(ConfirmationService.class),
                Mockito.mock(WarmupEffectsService.class), Mockito.mock(RegionService.class));

        Server server = Mockito.mock(Server.class);
        Mockito.doReturn(List.of()).when(server).getOnlinePlayers();
        Player op = Mockito.mock(Player.class);
        Mockito.when(op.getServer()).thenReturn(server);
        Mockito.when(op.isOp()).thenReturn(true);
        Player exempt = Mockito.mock(Player.class);
        Mockito.when(exempt.getServer()).thenReturn(server);
        Mockito.when(exempt.hasPermission("booscooldowns.exception")).thenReturn(true);

        for (Player player : new Player[] {op, exempt}) {
            PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, "/scut");
            listener.onCommand(event);
            assertEquals("/stonecutter", event.getMessage());
        }
    }
}
