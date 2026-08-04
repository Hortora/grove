package io.hortora.grove.qdrant;

public record VectorEntry(
        String id,
        String title,
        String sourceDocumentId,
        float[] vector) {
}