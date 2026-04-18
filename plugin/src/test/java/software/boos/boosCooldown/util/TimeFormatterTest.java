package software.boos.boosCooldown.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeFormatterTest {

    @Test
    void parseSecondsPlainNumber() {
        assertEquals(42L, TimeFormatter.parseSeconds("42"));
    }

    @Test
    void parseSecondsWithUnit() {
        assertEquals(5L, TimeFormatter.parseSeconds("5 seconds"));
        assertEquals(300L, TimeFormatter.parseSeconds("5 minutes"));
        assertEquals(7200L, TimeFormatter.parseSeconds("2 hours"));
        assertEquals(86400L, TimeFormatter.parseSeconds("1 day"));
    }

    @Test
    void parseSecondsComposite() {
        assertEquals(3660L, TimeFormatter.parseSeconds("1 hour 1 minute"));
        assertEquals(90L, TimeFormatter.parseSeconds("1m 30s"));
    }

    @Test
    void parseSecondsEmptyIsZero() {
        assertEquals(0L, TimeFormatter.parseSeconds(null));
        assertEquals(0L, TimeFormatter.parseSeconds(""));
        assertEquals(0L, TimeFormatter.parseSeconds("   "));
    }

    @Test
    void parseFutureTimestampRelative() {
        long before = System.currentTimeMillis();
        long result = TimeFormatter.parseFutureTimestampMillis("+2h");
        long after = System.currentTimeMillis();
        assertTrue(result >= before + 2 * 3600 * 1000L);
        assertTrue(result <= after + 2 * 3600 * 1000L + 100L);
    }

    @Test
    void parseFutureTimestampAbsolute() {
        long result = TimeFormatter.parseFutureTimestampMillis("2099-01-01 00:00:00");
        long expected = LocalDateTime.of(2099, 1, 1, 0, 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, result);
    }

    @Test
    void parseFutureTimestampInvalidThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> TimeFormatter.parseFutureTimestampMillis("not-a-date"));
        assertThrows(IllegalArgumentException.class,
                () -> TimeFormatter.parseFutureTimestampMillis(""));
    }

    @Test
    void formatRemainingShowsSeconds() {
        assertEquals("5 seconds",
                TimeFormatter.formatRemaining(5, "hours", "minutes", "seconds"));
    }

    @Test
    void formatRemainingShowsMinutesAndSeconds() {
        assertEquals("1 minutes, 30 seconds",
                TimeFormatter.formatRemaining(90, "hours", "minutes", "seconds"));
    }

    @Test
    void formatRemainingShowsHoursMinutesSeconds() {
        assertEquals("2 hours, 1 minutes, 30 seconds",
                TimeFormatter.formatRemaining(7290, "hours", "minutes", "seconds"));
    }

    @Test
    void formatRemainingTreatsZeroAsOneSecond() {
        // original behaviour - avoid "0 seconds" display
        String out = TimeFormatter.formatRemaining(0, "h", "m", "s");
        assertTrue(out.contains("1 s"));
    }
}
