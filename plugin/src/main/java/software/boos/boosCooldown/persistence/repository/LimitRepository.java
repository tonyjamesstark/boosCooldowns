package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.model.LimitEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LimitRepository {

    Optional<LimitEntry> find(UUID playerId, String commandKey);

    List<LimitEntry> findAllForPlayer(UUID playerId);

    void upsert(LimitEntry entry);

    void delete(UUID playerId, String commandKey);

    int deleteAllForPlayer(UUID playerId);

    int deleteAllForCommand(String commandKey);
}
