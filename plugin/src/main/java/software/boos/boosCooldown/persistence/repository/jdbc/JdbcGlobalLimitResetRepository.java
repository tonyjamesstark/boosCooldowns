package software.boos.boosCooldown.persistence.repository.jdbc;

import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.GlobalLimitResetRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JdbcGlobalLimitResetRepository implements GlobalLimitResetRepository {

    private final DataSource dataSource;
    private final String upsertSql;

    public JdbcGlobalLimitResetRepository(DataSource dataSource, StorageBackend backend) {
        this.dataSource = dataSource;
        this.upsertSql = switch (backend) {
            case SQLITE -> "INSERT INTO global_limits (command_key, reset_at) VALUES (?, ?) "
                    + "ON CONFLICT(command_key) DO UPDATE SET reset_at = excluded.reset_at";
            case MYSQL -> "INSERT INTO global_limits (command_key, reset_at) VALUES (?, ?) "
                    + "ON DUPLICATE KEY UPDATE reset_at = VALUES(reset_at)";
        };
    }

    @Override
    public Optional<Instant> find(String commandKey) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT reset_at FROM global_limits WHERE command_key = ?")) {
            ps.setString(1, commandKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(Instant.ofEpochMilli(rs.getLong(1)));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("find global reset", e);
        }
        return Optional.empty();
    }

    @Override
    public Map<String, Instant> findAll() {
        Map<String, Instant> result = new HashMap<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT command_key, reset_at FROM global_limits");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString(1), Instant.ofEpochMilli(rs.getLong(2)));
            }
        } catch (SQLException e) {
            throw new RepositoryException("findAll global reset", e);
        }
        return result;
    }

    @Override
    public List<String> findDue(Instant now) {
        List<String> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT command_key FROM global_limits WHERE reset_at <= ?")) {
            ps.setLong(1, now.toEpochMilli());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("findDue global reset", e);
        }
        return result;
    }

    @Override
    public void schedule(String commandKey, Instant resetAt) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(upsertSql)) {
            ps.setString(1, commandKey);
            ps.setLong(2, resetAt.toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("schedule global reset", e);
        }
    }

    @Override
    public void delete(String commandKey) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM global_limits WHERE command_key = ?")) {
            ps.setString(1, commandKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("delete global reset", e);
        }
    }
}
