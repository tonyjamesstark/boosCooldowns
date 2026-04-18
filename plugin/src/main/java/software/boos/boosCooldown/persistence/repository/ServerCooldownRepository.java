package software.boos.boosCooldown.persistence.repository;

import software.boos.boosCooldown.model.ServerCooldownEntry;

import java.util.Optional;

public interface ServerCooldownRepository {

    Optional<ServerCooldownEntry> find(String commandKey);

    void upsert(ServerCooldownEntry entry);

    void delete(String commandKey);
}
