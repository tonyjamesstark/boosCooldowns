package software.boos.boosCooldown.model;

import java.time.Instant;
import java.util.UUID;

public record LimitEntry(UUID playerId, String commandKey, int remainingUses, Instant resetAt) {
}
