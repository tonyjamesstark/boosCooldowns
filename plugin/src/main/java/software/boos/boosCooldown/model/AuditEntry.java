package software.boos.boosCooldown.model;

import java.time.Instant;
import java.util.UUID;

public record AuditEntry(long id, Instant at, UUID playerId, String playerName,
                         String command, String outcome) {

    public enum Outcome {
        ALLOWED, COOLDOWN_BLOCKED, LIMIT_BLOCKED, DISABLED, INSUFFICIENT_FUNDS,
        SERVER_COOLDOWN, PERMISSION_DENIED, WARMUP_STARTED, WARMUP_CANCELLED,
        CONFIRMATION_REQUIRED, CONFIRMATION_GRANTED, CONFIRMATION_DECLINED
    }
}
