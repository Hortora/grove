package io.hortora.grove.curation;

import io.hortora.grove.config.GroveConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

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
        Path filePath = validatePath(sourceDocumentId);
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
        Path filePath = validatePath(sourceDocumentId);
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
        Path filePath = validatePath(sourceDocumentId);
        if (!Files.exists(filePath)) {
            throw new IOException("Garden entry not found: " + sourceDocumentId);
        }

        Files.writeString(filePath, updatedContent);

        String geId = extractGeId(sourceDocumentId);
        commitChange(sourceDocumentId, "grove: edit " + geId);
    }

    public void moveDomain(String sourceDocumentId, String targetDomain) throws IOException, GitAPIException {
        Path sourcePath = validatePath(sourceDocumentId);
        if (!Files.exists(sourcePath)) {
            throw new IOException("Garden entry not found: " + sourceDocumentId);
        }

        String fileName  = sourcePath.getFileName().toString();
        Path   targetDir = gardenPath.resolve(targetDomain).normalize();
        if (!targetDir.startsWith(gardenPath)) {
            throw new IOException("Path traversal rejected: " + targetDomain);
        }

        Files.createDirectories(targetDir);
        Path targetPath = targetDir.resolve(fileName);
        if (Files.exists(targetPath)) {
            throw new IOException("Entry already exists in target domain: " + targetDomain + "/" + fileName);
        }

        Files.move(sourcePath, targetPath);

        String sourceDomain   = sourcePath.getParent().getFileName().toString();
        String geId           = extractGeId(sourceDocumentId);
        String targetRelative = targetDomain + "/" + fileName;

        try (Git git = Git.open(gardenPath.toFile())) {
            git.rm().addFilepattern(sourceDocumentId).call();
            git.add().addFilepattern(targetRelative).call();
            git.commit().setMessage("grove: move " + geId + " from " + sourceDomain + " to " + targetDomain).call();
        }
    }

    public int bulkConfirmFreshness(java.util.List<String> sourceDocumentIds) throws IOException, GitAPIException {
        String today = LocalDate.now().toString();
        int    count = 0;
        try (Git git = Git.open(gardenPath.toFile())) {
            for (String sourceDocumentId : sourceDocumentIds) {
                Path filePath = validatePath(sourceDocumentId);
                if (!Files.exists(filePath)) {continue;}

                String content = Files.readString(filePath);
                String updated;
                if (content.contains("last_reviewed:")) {
                    updated = content.replaceFirst("last_reviewed:.*", "last_reviewed: " + today);
                } else {
                    int endOfFrontmatter = content.indexOf("\n---", 3);
                    if (endOfFrontmatter > 0) {
                        updated = content.substring(0, endOfFrontmatter) + "\nlast_reviewed: " + today + content.substring(endOfFrontmatter);
                    } else {
                        continue;
                    }
                }
                Files.writeString(filePath, updated);
                git.add().addFilepattern(sourceDocumentId).call();
                count++;
            }
            if (count > 0) {
                git.commit().setMessage("grove: bulk confirm " + count + " entries").call();
            }
        }
        return count;
    }

    public int bulkRetire(java.util.List<String> sourceDocumentIds, String reason) throws IOException, GitAPIException {
        String today  = LocalDate.now().toString();
        String marker = "**Deprecated:** " + reason + " — " + today;
        int    count  = 0;
        try (Git git = Git.open(gardenPath.toFile())) {
            for (String sourceDocumentId : sourceDocumentIds) {
                Path filePath = validatePath(sourceDocumentId);
                if (!Files.exists(filePath)) {continue;}

                String content          = Files.readString(filePath);
                int    endOfFrontmatter = content.indexOf("\n---", 3);
                if (endOfFrontmatter > 0) {
                    int    bodyStart = endOfFrontmatter + 4;
                    String updated   = content.substring(0, bodyStart) + "\n\n" + marker + "\n" + content.substring(bodyStart);
                    Files.writeString(filePath, updated);
                } else {
                    Files.writeString(filePath, marker + "\n\n" + content);
                }
                git.add().addFilepattern(sourceDocumentId).call();
                count++;
            }
            if (count > 0) {
                git.commit().setMessage("grove: bulk retire " + count + " entries — " + reason).call();
            }
        }
        return count;
    }

    public int bulkRetag(java.util.List<String> sourceDocumentIds, java.util.List<String> addTags, java.util.List<String> removeTags) throws IOException, GitAPIException {
        int count = 0;
        try (Git git = Git.open(gardenPath.toFile())) {
            for (String sourceDocumentId : sourceDocumentIds) {
                Path filePath = validatePath(sourceDocumentId);
                if (!Files.exists(filePath)) {continue;}

                String content          = Files.readString(filePath);
                int    endOfFrontmatter = content.indexOf("\n---", 3);
                if (endOfFrontmatter <= 0) {continue;}

                String frontmatter = content.substring(0, endOfFrontmatter);
                String rest        = content.substring(endOfFrontmatter);

                java.util.Set<String> tags       = new java.util.LinkedHashSet<>();
                var                   tagMatcher = java.util.regex.Pattern.compile("tags:\\s*\\[([^\\]]*)]").matcher(frontmatter);
                if (tagMatcher.find()) {
                    for (String t : tagMatcher.group(1).split(",")) {
                        String trimmed = t.trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
                        if (!trimmed.isEmpty()) {tags.add(trimmed);}
                    }
                }

                if (removeTags != null) {tags.removeAll(removeTags);}
                if (addTags != null) {tags.addAll(addTags);}

                String tagLine = "tags: [" + String.join(", ", tags) + "]";
                String updatedFrontmatter;
                if (tagMatcher.find(0)) {
                    updatedFrontmatter = tagMatcher.replaceFirst(tagLine);
                } else {
                    updatedFrontmatter = frontmatter + "\n" + tagLine;
                }

                Files.writeString(filePath, updatedFrontmatter + rest);
                git.add().addFilepattern(sourceDocumentId).call();
                count++;
            }
            if (count > 0) {
                String desc = "";
                if (addTags != null && !addTags.isEmpty()) {desc += "+" + String.join(",", addTags);}
                if (removeTags != null && !removeTags.isEmpty()) {desc += " -" + String.join(",", removeTags);}
                git.commit().setMessage("grove: bulk retag " + count + " entries " + desc.trim()).call();
            }
        }
        return count;
    }


    private Path validatePath(String sourceDocumentId) throws IOException {
        Path resolved = gardenPath.resolve(sourceDocumentId).normalize();
        if (!resolved.startsWith(gardenPath)) {
            throw new IOException("Path traversal rejected: " + sourceDocumentId);
        }
        return resolved;
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
