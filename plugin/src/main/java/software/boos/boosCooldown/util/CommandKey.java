package software.boos.boosCooldown.util;

import java.util.Locale;

public final class CommandKey {

    private CommandKey() {
    }

    /**
     * Normalizes a raw command string into a stable, bounded key suitable for
     * DB persistence. Unlike {@code String.hashCode()} the result is stable
     * across JVM restarts and unique per distinct command.
     */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return "";
        }
        if (!lower.startsWith("/") && !lower.equals("*")) {
            lower = "/" + lower;
        }
        lower = lower.replaceAll("\\s+", " ");
        if (lower.length() > 191) {
            lower = lower.substring(0, 191);
        }
        return lower;
    }
}
