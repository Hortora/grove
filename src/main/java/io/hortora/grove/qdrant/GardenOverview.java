package io.hortora.grove.qdrant;

public record GardenOverview(
        int totalEntries,
        int totalDomains,
        int staleCount,
        int untaggedCount,
        int neverRetrievedCount) {
}
