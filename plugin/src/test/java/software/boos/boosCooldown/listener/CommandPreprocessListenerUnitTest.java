package software.boos.boosCooldown.listener;

import org.junit.jupiter.api.Test;

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
}
