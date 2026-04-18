package software.boos.boosCooldown.persistence.repository.jdbc;

import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.UserPreferencesRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class JdbcUserPreferencesRepository implements UserPreferencesRepository {

    private final DataSource dataSource;
    private final String upsertSql;

    public JdbcUserPreferencesRepository(DataSource dataSource, StorageBackend backend) {
        this.dataSource = dataSource;
        this.upsertSql = switch (backend) {
            case SQLITE -> "INSERT INTO user_preferences (player_uuid, pref_key, pref_value) VALUES (?, ?, ?) "
                    + "ON CONFLICT(player_uuid, pref_key) DO UPDATE SET pref_value = excluded.pref_value";
            case MYSQL -> "INSERT INTO user_preferences (player_uuid, pref_key, pref_value) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE pref_value = VALUES(pref_value)";
        };
    }

    @Override
    public Optional<String> find(UUID playerId, String key) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT pref_value FROM user_preferences WHERE player_uuid = ? AND pref_key = ?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("find user preference", e);
        }
        return Optional.empty();
    }

    @Override
    public void upsert(UUID playerId, String key, String value) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(upsertSql)) {
            ps.setString(1, playerId.toString());
            ps.setString(2, key);
            ps.setString(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("upsert user preference", e);
        }
    }

    @Override
    public void delete(UUID playerId, String key) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM user_preferences WHERE player_uuid = ? AND pref_key = ?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("delete user preference", e);
        }
    }
}
