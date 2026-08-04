package io.hortora.grove.analysis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.hortora.grove.config.GroveConfig;
import io.hortora.grove.qdrant.QdrantGardenClient;
import io.hortora.grove.qdrant.VectorEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DuplicateDetector {

    private static final double THRESHOLD = 0.92;
    private static final Pattern GE_ID_PATTERN = Pattern.compile("(GE-[^./]+)");

    private final QdrantGardenClient client;
    private final String gardenDbPath;
    private final String groveDbPath;

    @Inject
    public DuplicateDetector(QdrantGardenClient client, GroveConfig config) {
        this.client = client;
        this.gardenDbPath = config.gardenDb().path();
        this.groveDbPath = config.garden().path() + "/../grove.db";
    }

    public DuplicateDetector(QdrantGardenClient client, String gardenDbPath, String groveDbPath) {
        this.client = client;
        this.gardenDbPath = gardenDbPath;
        this.groveDbPath = groveDbPath;
    }

    public List<DuplicatePair> analyse(String domain) {
        List<VectorEntry> entries = client.fetchEntriesWithVectors(domain);
        Set<String> checkedPairs = loadCheckedPairs();
        List<DuplicatePair> pairs = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                VectorEntry a = entries.get(i);
                VectorEntry b = entries.get(j);

                String geIdA = extractGeId(a.sourceDocumentId());
                String geIdB = extractGeId(b.sourceDocumentId());
                String pairKey = pairKey(geIdA, geIdB);

                if (checkedPairs.contains(pairKey)) continue;

                double sim = cosineSimilarity(a.vector(), b.vector());
                if (sim >= THRESHOLD) {
                    pairs.add(new DuplicatePair(
                            a.id(), a.title(), a.sourceDocumentId(),
                            b.id(), b.title(), b.sourceDocumentId(),
                            sim));
                }
            }
        }

        pairs.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));
        cacheResults(domain, pairs);
        return pairs;
    }

    public List<DuplicatePair> getCached(String domain) {
        ensureGroveDb();
        List<DuplicatePair> pairs = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + groveDbPath)) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT entry_a, title_a, source_doc_a, entry_b, title_b, source_doc_b, similarity FROM duplicate_pairs WHERE domain = ? ORDER BY similarity DESC");
            ps.setString(1, domain);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                pairs.add(new DuplicatePair(
                        rs.getString("entry_a"), rs.getString("title_a"), rs.getString("source_doc_a"),
                        rs.getString("entry_b"), rs.getString("title_b"), rs.getString("source_doc_b"),
                        rs.getDouble("similarity")));
            }
        } catch (SQLException e) {
            // return empty
        }
        return pairs;
    }

    static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    private Set<String> loadCheckedPairs() {
        Set<String> pairs = new HashSet<>();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + gardenDbPath)) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT pair FROM checked_pairs");
            while (rs.next()) {
                pairs.add(rs.getString("pair"));
            }
        } catch (SQLException e) {
            // no checked pairs available
        }
        return pairs;
    }

    private void cacheResults(String domain, List<DuplicatePair> pairs) {
        ensureGroveDb();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + groveDbPath)) {
            PreparedStatement del = conn.prepareStatement("DELETE FROM duplicate_pairs WHERE domain = ?");
            del.setString(1, domain);
            del.execute();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO duplicate_pairs (domain, entry_a, title_a, source_doc_a, entry_b, title_b, source_doc_b, similarity, analysed_at) VALUES (?,?,?,?,?,?,?,?,?)");
            String now = Instant.now().toString();
            for (DuplicatePair p : pairs) {
                ps.setString(1, domain);
                ps.setString(2, p.entryA());
                ps.setString(3, p.titleA());
                ps.setString(4, p.sourceDocIdA());
                ps.setString(5, p.entryB());
                ps.setString(6, p.titleB());
                ps.setString(7, p.sourceDocIdB());
                ps.setDouble(8, p.similarity());
                ps.setString(9, now);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to cache duplicate results", e);
        }
    }

    private void ensureGroveDb() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + groveDbPath)) {
            Statement st = conn.createStatement();
            st.execute("""
                    CREATE TABLE IF NOT EXISTS duplicate_pairs (
                        domain TEXT NOT NULL,
                        entry_a TEXT NOT NULL,
                        title_a TEXT,
                        source_doc_a TEXT,
                        entry_b TEXT NOT NULL,
                        title_b TEXT,
                        source_doc_b TEXT,
                        similarity REAL NOT NULL,
                        analysed_at TEXT NOT NULL
                    )""");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize grove.db", e);
        }
    }

    private String extractGeId(String sourceDocumentId) {
        if (sourceDocumentId == null) return "unknown";
        Matcher m = GE_ID_PATTERN.matcher(sourceDocumentId);
        return m.find() ? m.group(1) : sourceDocumentId;
    }

    private String pairKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + " × " + b : b + " × " + a;
    }
}