package software.boos.boosCooldown.model;

import java.time.Instant;

/** Snapshot of a single node's config state, as visible to other nodes in the shared DB. */
public record ConfigVersion(String nodeId, String contentHash, Instant reloadedAt) {
}
