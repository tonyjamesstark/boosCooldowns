package software.boos.boosCooldown.model;

import java.time.Instant;
import java.util.UUID;

public record CooldownEntry(UUID playerId, String commandKey, Instant expiresAt) {

    public boolean isActive(Instant now) {
        return expiresAt.isAfter(now);
    }
}
