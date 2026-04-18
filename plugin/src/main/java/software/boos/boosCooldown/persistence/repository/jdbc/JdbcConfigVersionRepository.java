package software.boos.boosCooldown.persistence.repository.jdbc;

import software.boos.boosCooldown.model.ConfigVersion;
import software.boos.boosCooldown.persistence.StorageBackend;
import software.boos.boosCooldown.persistence.repository.ConfigVersionRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcConfigVersionRepository implements ConfigVersionRepository {

    private final DataSource dataSource;
    private final String upsertSql;

    public JdbcConfigVersionRepository(DataSource dataSource, StorageBackend backend) {
        this.dataSource = dataSource;
        this.upsertSql = switch (backend) {
            case SQLITE -> "INSERT INTO config_version (node_id, content_hash, reloaded_at) VALUES (?, ?, ?) "
                    + "ON CONFLICT(node_id) DO UPDATE SET content_hash = excluded.content_hash, "
                    + "reloaded_at = excluded.reloaded_at";
            case MYSQL -> "INSERT INTO config_version (node_id, content_hash, reloaded_at) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE content_hash = VALUES(content_hash), "
                    + "reloaded_at = VALUES(reloaded_at)";
        };
    }

    @Override
    public void upsert(String nodeId, String contentHash, Instant reloadedAt) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(upsertSql)) {
            ps.setString(1, nodeId);
            ps.setString(2, contentHash);
            ps.setLong(3, reloadedAt.toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("upsert config_version", e);
        }
    }

    @Override
    public List<ConfigVersion> findAll() {
        List<ConfigVersion> result = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT node_id, content_hash, reloaded_at FROM config_version "
                             + "ORDER BY reloaded_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new ConfigVersion(
                        rs.getString(1),
                        rs.getString(2),
                        Instant.ofEpochMilli(rs.getLong(3))));
            }
        } catch (SQLException e) {
            throw new RepositoryException("findAll config_version", e);
        }
        return result;
    }

    @Override
    public Optional<ConfigVersion> find(String nodeId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT content_hash, reloaded_at FROM config_version WHERE node_id = ?")) {
            ps.setString(1, nodeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ConfigVersion(nodeId, rs.getString(1),
                            Instant.ofEpochMilli(rs.getLong(2))));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("find config_version", e);
        }
        return Optional.empty();
    }

    @Override
    public void delete(String nodeId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM config_version WHERE node_id = ?")) {
            ps.setString(1, nodeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("delete config_version", e);
        }
    }
}
