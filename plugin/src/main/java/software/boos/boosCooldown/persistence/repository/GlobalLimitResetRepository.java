package software.boos.boosCooldown.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GlobalLimitResetRepository {

    Optional<Instant> find(String commandKey);

    Map<String, Instant> findAll();

    List<String> findDue(Instant now);

    void schedule(String commandKey, Instant resetAt);

    void delete(String commandKey);
}
