package software.boos.boosCooldown.persistence.repository.jdbc;

import software.boos.boosCooldown.model.ServerCooldownEntry;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.ServerCooldownRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public final class JdbcServerCooldownRepository implements ServerCooldownRepository {

    private final DataSource dataSource;
    private final String upsertSql;

    public JdbcServerCooldownRepository(DataSource dataSource, StorageBackend backend) {
        this.dataSource = dataSource;
        this.upsertSql = switch (backend) {
            case SQLITE -> "INSERT INTO server_cooldowns (command_key, expires_at) VALUES (?, ?) "
                    + "ON CONFLICT(command_key) DO UPDATE SET expires_at = excluded.expires_at";
            case MYSQL -> "INSERT INTO server_cooldowns (command_key, expires_at) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE expires_at = VALUES(expires_at)";
        };
    }

    @Override
    public Optional<ServerCooldownEntry> find(String commandKey) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT expires_at FROM server_cooldowns WHERE command_key = ?")) {
            ps.setString(1, commandKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ServerCooldownEntry(commandKey,
                            Instant.ofEpochMilli(rs.getLong(1))));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("find server cooldown", e);
        }
        return Optional.empty();
    }

    @Override
    public void upsert(ServerCooldownEntry entry) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(upsertSql)) {
            ps.setString(1, entry.commandKey());
            ps.setLong(2, entry.expiresAt().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("upsert server cooldown", e);
        }
    }

    @Override
    public void delete(String commandKey) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM server_cooldowns WHERE command_key = ?")) {
            ps.setString(1, commandKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("delete server cooldown", e);
        }
    }
}
