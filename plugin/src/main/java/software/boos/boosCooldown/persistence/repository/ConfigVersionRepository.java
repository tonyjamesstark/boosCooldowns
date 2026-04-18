package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.model.ConfigVersion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConfigVersionRepository {

    /** Record (or update) this node's current hash + reload timestamp. */
    void upsert(String nodeId, String contentHash, Instant reloadedAt);

    /** All known nodes, most recently reloaded first. */
    List<ConfigVersion> findAll();

    Optional<ConfigVersion> find(String nodeId);

    void delete(String nodeId);
}
