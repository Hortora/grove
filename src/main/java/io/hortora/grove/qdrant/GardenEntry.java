package io.hortora.grove.qdrant;

public record GardenEntry(
        String id,
        String title,
        String type,
        String domain,
        double score,
        String submitted,
        String sourceDocumentId,
        String content) {
}
