package software.boos.boosCooldown.persistence.repository.jdbc;

import software.boos.boosCooldown.model.CooldownEntry;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.CooldownRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcCooldownRepository implements CooldownRepository {

    private final DataSource dataSource;
    private final String upsertSql;

    public JdbcCooldownRepository(DataSource dataSource, StorageBackend backend) {
        this.dataSource = dataSource;
        this.upsertSql = switch (backend) {
            case SQLITE -> "INSERT INTO cooldowns (player_uuid, command_key, expires_at) VALUES (?, ?, ?) "
                    + "ON CONFLICT(player_uuid, command_key) DO UPDATE SET expires_at = excluded.expires_at";
            case MYSQL -> "INSERT INTO cooldowns (player_uuid, command_key, expires_at) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE expires_at = VALUES(expires_at)";
        };
    }

    @Override
    public Optional<CooldownEntry> find(UUID playerId, String commandKey) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT expires_at FROM cooldowns WHERE player_uuid = ? AND command_key = ?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, commandKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new CooldownEntry(playerId, commandKey,
                            Instant.ofEpochMilli(rs.getLong(1))));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("find cooldown", e);
        }
        return Optional.empty();
    }

    @Override
    public List<CooldownEntry> findAllForPlayer(UUID playerId) {
        List<CooldownEntry> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT command_key, expires_at FROM cooldowns WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new CooldownEntry(playerId, rs.getString(1),
                            Instant.ofEpochMilli(rs.getLong(2))));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("findAllForPlayer cooldown", e);
        }
        return result;
    }

    @Override
    public void upsert(CooldownEntry entry) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(upsertSql)) {
            ps.setString(1, entry.playerId().toString());
            ps.setString(2, entry.commandKey());
            ps.setLong(3, entry.expiresAt().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("upsert cooldown", e);
        }
    }

    @Override
    public void delete(UUID playerId, String commandKey) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM cooldowns WHERE player_uuid = ? AND command_key = ?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, commandKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("delete cooldown", e);
        }
    }

    @Override
    public int deleteAllForPlayer(UUID playerId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM cooldowns WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("deleteAllForPlayer cooldown", e);
        }
    }

    @Override
    public int deleteExpired(Instant cutoff) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM cooldowns WHERE expires_at < ?")) {
            ps.setLong(1, cutoff.toEpochMilli());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("deleteExpired cooldown", e);
        }
    }
}
