package io.hortora.grove.qdrant;

import java.util.Map;

public record DomainStats(
        String domain,
        int entryCount,
        Map<String, Integer> typeBreakdown,
        double averageScore,
        int retrievedEntryCount,
        int neverRetrievedCount) {
}
