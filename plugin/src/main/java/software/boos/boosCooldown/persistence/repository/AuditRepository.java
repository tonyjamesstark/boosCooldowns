package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.model.AuditEntry;

import java.time.Instant;
import java.util.List;

public interface AuditRepository {

    void record(Instant at, java.util.UUID playerId, String playerName,
                String command, String outcome);

    List<AuditEntry> findRecent(int limit);

    List<AuditEntry> findForCommand(String command, int limit);

    int deleteOlderThan(Instant cutoff);
}
