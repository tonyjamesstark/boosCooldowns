package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.model.CooldownEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CooldownRepository {

    Optional<CooldownEntry> find(UUID playerId, String commandKey);

    List<CooldownEntry> findAllForPlayer(UUID playerId);

    void upsert(CooldownEntry entry);

    void delete(UUID playerId, String commandKey);

    int deleteAllForPlayer(UUID playerId);

    int deleteExpired(Instant cutoff);
}
