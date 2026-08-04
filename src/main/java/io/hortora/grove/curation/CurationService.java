package io.hortora.grove.curation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import io.hortora.grove.config.GroveConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CurationService {

    private final Path gardenPath;

    @Inject
    public CurationService(GroveConfig config) {
        this(Path.of(config.garden().path()));
    }

    public CurationService(Path gardenPath) {
        this.gardenPath = gardenPath;
    }

    public void confirmFreshness(String sourceDocumentId) throws IOException, GitAPIException {
        Path filePath = gardenPath.resolve(sourceDocumentId);
        if (!Files.exists(filePath)) {
            throw new IOException("Garden entry not found: " + sourceDocumentId);
        }

        String content = Files.readString(filePath);
        String today = LocalDate.now().toString();

        String updated;
        if (content.contains("last_reviewed:")) {
            updated = content.replaceFirst("last_reviewed:.*", "last_reviewed: " + today);
        } else {
            int endOfFrontmatter = content.indexOf("\n---", 3);
            if (endOfFrontmatter > 0) {
                updated = content.substring(0, endOfFrontmatter) + "\nlast_reviewed: " + today + content.substring(endOfFrontmatter);
            } else {
                throw new IOException("No frontmatter found in " + sourceDocumentId);
            }
        }

        Files.writeString(filePath, updated);

        String geId = extractGeId(sourceDocumentId);
        commitChange(sourceDocumentId, "grove: confirm freshness " + geId);
    }

    public void retire(String sourceDocumentId, String reason) throws IOException, GitAPIException {
        Path filePath = gardenPath.resolve(sourceDocumentId);
        if (!Files.exists(filePath)) {
            throw new IOException("Garden entry not found: " + sourceDocumentId);
        }

        String content = Files.readString(filePath);
        String today = LocalDate.now().toString();
        String marker = "**Deprecated:** " + reason + " — " + today;

        int endOfFrontmatter = content.indexOf("\n---", 3);
        if (endOfFrontmatter > 0) {
            int bodyStart = endOfFrontmatter + 4;
            String updated = content.substring(0, bodyStart) + "\n\n" + marker + "\n" + content.substring(bodyStart);
            Files.writeString(filePath, updated);
        } else {
            Files.writeString(filePath, marker + "\n\n" + content);
        }

        String geId = extractGeId(sourceDocumentId);
        commitChange(sourceDocumentId, "grove: retire " + geId + " — " + reason);
    }

    public void editEntry(String sourceDocumentId, String updatedContent) throws IOException, GitAPIException {
        Path filePath = gardenPath.resolve(sourceDocumentId);
        if (!Files.exists(filePath)) {
            throw new IOException("Garden entry not found: " + sourceDocumentId);
        }

        Files.writeString(filePath, updatedContent);

        String geId = extractGeId(sourceDocumentId);
        commitChange(sourceDocumentId, "grove: edit " + geId);
    }

    private void commitChange(String relativePath, String message) throws IOException, GitAPIException {
        try (Git git = Git.open(gardenPath.toFile())) {
            git.add().addFilepattern(relativePath).call();
            git.commit().setMessage(message).call();
        }
    }

    private String extractGeId(String sourceDocumentId) {
        if (sourceDocumentId == null) return "unknown";
        var match = java.util.regex.Pattern.compile("(GE-[^.]+)").matcher(sourceDocumentId);
        return match.find() ? match.group(1) : sourceDocumentId;
    }
}
