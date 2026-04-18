package software.boos.boosCooldown.persistence.repository.jdbc;

import software.boos.boosCooldown.model.AuditEntry;
import software.boos.boosCooldown.persistence.repository.AuditRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class JdbcAuditRepository implements AuditRepository {

    private final DataSource dataSource;

    public JdbcAuditRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void record(Instant at, UUID playerId, String playerName,
                       String command, String outcome) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO command_audit (at_millis, player_uuid, player_name, command, outcome) "
                             + "VALUES (?, ?, ?, ?, ?)")) {
            ps.setLong(1, at.toEpochMilli());
            ps.setString(2, playerId.toString());
            ps.setString(3, playerName);
            ps.setString(4, command);
            ps.setString(5, outcome);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("record audit", e);
        }
    }

    @Override
    public List<AuditEntry> findRecent(int limit) {
        return query("SELECT id, at_millis, player_uuid, player_name, command, outcome "
                + "FROM command_audit ORDER BY at_millis DESC LIMIT ?", limit, null);
    }

    @Override
    public List<AuditEntry> findForCommand(String command, int limit) {
        return query("SELECT id, at_millis, player_uuid, player_name, command, outcome "
                + "FROM command_audit WHERE command = ? ORDER BY at_millis DESC LIMIT ?",
                limit, command);
    }

    @Override
    public int deleteOlderThan(Instant cutoff) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM command_audit WHERE at_millis < ?")) {
            ps.setLong(1, cutoff.toEpochMilli());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("delete audit", e);
        }
    }

    private List<AuditEntry> query(String sql, int limit, String command) {
        List<AuditEntry> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (command != null) {
                ps.setString(1, command);
                ps.setInt(2, limit);
            } else {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new AuditEntry(
                            rs.getLong(1),
                            Instant.ofEpochMilli(rs.getLong(2)),
                            UUID.fromString(rs.getString(3)),
                            rs.getString(4),
                            rs.getString(5),
                            rs.getString(6)));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("query audit", e);
        }
        return result;
    }
}
