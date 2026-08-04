package io.hortora.grove.tracking;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.hortora.grove.config.GroveConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RetrievalStatsService {

    private final String dbPath;

    @Inject
    public RetrievalStatsService(GroveConfig config) {
        this(config.retrievalTracking().path());
    }

    public RetrievalStatsService(String dbPath) {
        this.dbPath = dbPath;
    }

    public Map<String, Integer> getRetrievalCounts() {
        Map<String, Integer> counts = new HashMap<>();
        String sql = "SELECT source_document_id, COUNT(*) as cnt FROM retrieved_documents GROUP BY source_document_id";

        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("source_document_id"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query retrieval-tracking.db", e);
        }
        return counts;
    }

    public List<EntryRetrievalStats> getAllStats() {
        List<EntryRetrievalStats> stats = new ArrayList<>();
        String sql = """
                SELECT d.source_document_id,
                       COUNT(*) as cnt,
                       MAX(r.timestamp) as last_retrieved
                FROM retrieved_documents d
                JOIN retrieval_records r ON d.retrieval_id = r.retrieval_id
                GROUP BY d.source_document_id
                ORDER BY cnt DESC
                """;

        try (Connection conn = openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                stats.add(new EntryRetrievalStats(
                        rs.getString("source_document_id"),
                        rs.getInt("cnt"),
                        rs.getString("last_retrieved")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query retrieval-tracking.db", e);
        }
        return stats;
    }

    public Set<String> getNeverRetrieved(Set<String> allDocIds) {
        Set<String> retrieved = getRetrievalCounts().keySet();
        Set<String> neverRetrieved = new HashSet<>(allDocIds);
        neverRetrieved.removeAll(retrieved);
        return neverRetrieved;
    }

    private Connection openConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        var props = new java.util.Properties();
        props.setProperty("open_mode", "1");
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath, props);
    }
}
