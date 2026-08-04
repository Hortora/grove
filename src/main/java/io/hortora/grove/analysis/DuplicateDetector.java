package io.hortora.grove.analysis;

import io.hortora.grove.config.GroveConfig;
import io.hortora.grove.qdrant.QdrantGardenClient;
import io.hortora.grove.qdrant.VectorEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class DuplicateDetector {

    private static final double  THRESHOLD     = 0.92;
    private static final Pattern GE_ID_PATTERN = Pattern.compile("(GE-[^./]+)");

    private final QdrantGardenClient client;
    private final String             gardenDbPath;

    @Inject
    public DuplicateDetector(QdrantGardenClient client, GroveConfig config) {
        this.client       = client;
        this.gardenDbPath = config.gardenDb().path();
    }

    public DuplicateDetector(QdrantGardenClient client, String gardenDbPath) {
        this.client       = client;
        this.gardenDbPath = gardenDbPath;
    }

    public List<DuplicatePair> analyse(String domain) {
        List<VectorEntry>   entries      = client.fetchEntriesWithVectors(domain);
        Set<String>         checkedPairs = loadCheckedPairs();
        List<DuplicatePair> pairs        = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                VectorEntry a = entries.get(i);
                VectorEntry b = entries.get(j);

                String geIdA   = extractGeId(a.sourceDocumentId());
                String geIdB   = extractGeId(b.sourceDocumentId());
                String pairKey = pairKey(geIdA, geIdB);

                if (checkedPairs.contains(pairKey)) {continue;}

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
        return pairs;
    }

    static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {return 0;}
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

    private String extractGeId(String sourceDocumentId) {
        if (sourceDocumentId == null) {return "unknown";}
        Matcher m = GE_ID_PATTERN.matcher(sourceDocumentId);
        return m.find() ? m.group(1) : sourceDocumentId;
    }

    private String pairKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + " × " + b : b + " × " + a;
    }
}