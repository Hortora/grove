package io.hortora.grove.health;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.hortora.grove.config.GroveConfig;
import io.hortora.grove.qdrant.GardenEntry;
import io.hortora.grove.qdrant.QdrantGardenClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IndexReconciler {

    private final QdrantGardenClient qdrantClient;
    private final Path gardenPath;
    private final String gardenDbPath;

    @Inject
    public IndexReconciler(QdrantGardenClient qdrantClient, GroveConfig config) {
        this.qdrantClient = qdrantClient;
        this.gardenPath = Path.of(config.garden().path());
        this.gardenDbPath = config.gardenDb().path();
    }

    public ReconcileReport reconcile() {
        Set<String> qdrantIds = new HashSet<>();
        for (GardenEntry entry : qdrantClient.fetchAllEntries()) {
            if (entry.sourceDocumentId() != null) {
                qdrantIds.add(entry.sourceDocumentId());
            }
        }

        Set<String> fileIds = scanFilesystem();
        Set<String> dbIds = scanGardenDb();

        List<String> missingFromQdrant = new ArrayList<>();
        for (String fileId : fileIds) {
            if (!qdrantIds.contains(fileId)) {
                missingFromQdrant.add(fileId);
            }
        }

        List<String> missingFromDb = new ArrayList<>();
        for (String fileId : fileIds) {
            if (!dbIds.contains(fileId)) {
                missingFromDb.add(fileId);
            }
        }

        return new ReconcileReport(
                qdrantIds.size(),
                dbIds.size(),
                fileIds.size(),
                missingFromQdrant,
                missingFromDb);
    }

    private Set<String> scanFilesystem() {
        Set<String> ids = new HashSet<>();
        try (Stream<Path> walk = Files.walk(gardenPath)) {
            walk.filter(p -> p.getFileName().toString().startsWith("GE-") && p.toString().endsWith(".md"))
                    .forEach(p -> ids.add(gardenPath.relativize(p).toString()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan garden directory", e);
        }
        return ids;
    }

    private Set<String> scanGardenDb() {
        Set<String> ids = new HashSet<>();
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite driver not found", e);
        }

        String sql = "SELECT file_path FROM entries_index";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + gardenDbPath);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getString("file_path"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query garden.db", e);
        }
        return ids;
    }
}
