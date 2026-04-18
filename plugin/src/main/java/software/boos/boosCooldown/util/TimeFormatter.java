package software.boos.boosCooldown.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeFormatter {

    private static final DateTimeFormatter ABSOLUTE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern RELATIVE = Pattern.compile(
            "^\\+?(?<val>\\d+)\\s*(?<unit>s|sec|secs|seconds|m|min|mins|minute|minutes|h|hour|hours|d|day|days)$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern COMPOSITE = Pattern.compile(
            "(?<val>\\d+)\\s*(?<unit>s|sec|secs|seconds|m|min|mins|minute|minutes|h|hour|hours|d|day|days)",
            Pattern.CASE_INSENSITIVE);

    private TimeFormatter() {
    }

    /**
     * Parses a human-readable duration like "5 seconds", "2 hours 30 minutes",
     * "1d", or a plain integer (interpreted as seconds).
     *
     * @return duration in seconds, 0 if invalid
     */
    public static long parseSeconds(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String trimmed = raw.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            // fall through to composite parsing
        }

        long total = 0;
        Matcher matcher = COMPOSITE.matcher(trimmed);
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group("val"));
            total += toSeconds(value, matcher.group("unit").toLowerCase());
        }
        return total;
    }

    /**
     * Parses either an absolute timestamp "yyyy-MM-dd HH:mm:ss" or a relative
     * offset like "+2h", "+30m". Returns epoch millis.
     */
    public static long parseFutureTimestampMillis(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("empty timestamp");
        }
        String trimmed = raw.trim();
        Matcher rel = RELATIVE.matcher(trimmed);
        if (rel.matches()) {
            long value = Long.parseLong(rel.group("val"));
            long secs = toSeconds(value, rel.group("unit").toLowerCase());
            return System.currentTimeMillis() + secs * 1000L;
        }
        try {
            LocalDateTime dt = LocalDateTime.parse(trimmed, ABSOLUTE_FORMAT);
            return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid timestamp: " + raw
                    + " (expected 'yyyy-MM-dd HH:mm:ss' or relative like '+2h')");
        }
    }

    public static String formatRemaining(long seconds, String hoursUnit,
                                         String minutesUnit, String secondsUnit) {
        Duration d = Duration.ofSeconds(Math.max(0, seconds));
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long secs = d.toSecondsPart();
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(' ').append(hoursUnit).append(", ");
        }
        if (hours > 0 || minutes > 0) {
            sb.append(minutes).append(' ').append(minutesUnit).append(", ");
        }
        sb.append(secs <= 0 ? 1 : secs).append(' ').append(secondsUnit);
        return sb.toString();
    }

    private static long toSeconds(long value, String unit) {
        return switch (unit) {
            case "s", "sec", "secs", "second", "seconds" -> value;
            case "m", "min", "mins", "minute", "minutes" -> value * 60L;
            case "h", "hour", "hours" -> value * 3600L;
            case "d", "day", "days" -> value * 86400L;
            default -> 0L;
        };
    }
}
