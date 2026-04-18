package software.boos.boosCooldown.persistence.repository.jdbc;

import software.boos.boosCooldown.model.LimitEntry;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.LimitRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class JdbcLimitRepository implements LimitRepository {

    private final DataSource dataSource;
    private final String upsertSql;

    public JdbcLimitRepository(DataSource dataSource, StorageBackend backend) {
        this.dataSource = dataSource;
        this.upsertSql = switch (backend) {
            case SQLITE -> "INSERT INTO limit_uses (player_uuid, command_key, remaining_uses, reset_at) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(player_uuid, command_key) DO UPDATE SET remaining_uses = excluded.remaining_uses, reset_at = excluded.reset_at";
            case MYSQL -> "INSERT INTO limit_uses (player_uuid, command_key, remaining_uses, reset_at) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE remaining_uses = VALUES(remaining_uses), reset_at = VALUES(reset_at)";
        };
    }

    @Override
    public Optional<LimitEntry> find(UUID playerId, String commandKey) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT remaining_uses, reset_at FROM limit_uses WHERE player_uuid = ? AND command_key = ?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, commandKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(playerId, commandKey, rs));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("find limit", e);
        }
        return Optional.empty();
    }

    @Override
    public List<LimitEntry> findAllForPlayer(UUID playerId) {
        List<LimitEntry> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT command_key, remaining_uses, reset_at FROM limit_uses WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cmd = rs.getString(1);
                    int remaining = rs.getInt(2);
                    long resetMillis = rs.getLong(3);
                    Instant resetAt = rs.wasNull() ? null : Instant.ofEpochMilli(resetMillis);
                    result.add(new LimitEntry(playerId, cmd, remaining, resetAt));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("findAllForPlayer limit", e);
        }
        return result;
    }

    private LimitEntry mapRow(UUID playerId, String commandKey, ResultSet rs) throws SQLException {
        int remaining = rs.getInt(1);
        long resetMillis = rs.getLong(2);
        Instant resetAt = rs.wasNull() ? null : Instant.ofEpochMilli(resetMillis);
        return new LimitEntry(playerId, commandKey, remaining, resetAt);
    }

    @Override
    public void upsert(LimitEntry entry) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(upsertSql)) {
            ps.setString(1, entry.playerId().toString());
            ps.setString(2, entry.commandKey());
            ps.setInt(3, entry.remainingUses());
            if (entry.resetAt() == null) {
                ps.setNull(4, Types.BIGINT);
            } else {
                ps.setLong(4, entry.resetAt().toEpochMilli());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("upsert limit", e);
        }
    }

    @Override
    public void delete(UUID playerId, String commandKey) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM limit_uses WHERE player_uuid = ? AND command_key = ?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, commandKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("delete limit", e);
        }
    }

    @Override
    public int deleteAllForPlayer(UUID playerId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM limit_uses WHERE player_uuid = ?")) {
            ps.setString(1, playerId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("deleteAllForPlayer limit", e);
        }
    }

    @Override
    public int deleteAllForCommand(String commandKey) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM limit_uses WHERE command_key = ?")) {
            ps.setString(1, commandKey);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("deleteAllForCommand limit", e);
        }
    }
}
