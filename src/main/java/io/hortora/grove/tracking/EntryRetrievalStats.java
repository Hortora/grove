package io.hortora.grove.tracking;

public record EntryRetrievalStats(
        String sourceDocumentId,
        int retrievalCount,
        String lastRetrievedAt) {
}
