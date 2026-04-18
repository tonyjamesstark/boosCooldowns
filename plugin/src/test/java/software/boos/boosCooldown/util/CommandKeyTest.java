package software.boos.boosCooldown.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandKeyTest {

    @Test
    void normalizeEmptyReturnsEmpty() {
        assertEquals("", CommandKey.normalize(null));
        assertEquals("", CommandKey.normalize(""));
    }

    @Test
    void normalizeLowercases() {
        assertEquals("/kit", CommandKey.normalize("/KIT"));
        assertEquals("/kit", CommandKey.normalize("/Kit"));
    }

    @Test
    void normalizeAddsLeadingSlash() {
        assertEquals("/kit", CommandKey.normalize("kit"));
    }

    @Test
    void normalizePreservesWildcard() {
        assertEquals("*", CommandKey.normalize("*"));
    }

    @Test
    void normalizeCollapsesWhitespace() {
        assertEquals("/me hello world", CommandKey.normalize("  /me   hello    world  "));
    }

    @Test
    void normalizeTruncatesOversizeKey() {
        String huge = "/" + "a".repeat(300);
        assertEquals(191, CommandKey.normalize(huge).length());
    }

    @Test
    void normalizeIsIdempotent() {
        String once = CommandKey.normalize("/Kit example");
        assertEquals(once, CommandKey.normalize(once));
    }
}
