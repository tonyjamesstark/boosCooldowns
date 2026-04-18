package software.boos.boosCooldown.model;

import java.time.Instant;

public record ServerCooldownEntry(String commandKey, Instant expiresAt) {

    public boolean isActive(Instant now) {
        return expiresAt.isAfter(now);
    }
}
