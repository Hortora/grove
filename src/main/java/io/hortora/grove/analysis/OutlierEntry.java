package io.hortora.grove.analysis;

public record OutlierEntry(
        String entryId,
        String title,
        String sourceDocumentId,
        double distanceFromCentroid) {
}