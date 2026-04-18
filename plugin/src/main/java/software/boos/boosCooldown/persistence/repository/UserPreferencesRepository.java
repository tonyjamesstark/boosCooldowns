package software.boos.boosCooldown.persistence.repository;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferencesRepository {

    Optional<String> find(UUID playerId, String key);

    void upsert(UUID playerId, String key, String value);

    void delete(UUID playerId, String key);

    default boolean getBoolean(UUID playerId, String key, boolean defaultValue) {
        return find(playerId, key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    default void setBoolean(UUID playerId, String key, boolean value) {
        upsert(playerId, key, Boolean.toString(value));
    }
}
