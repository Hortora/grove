package io.hortora.grove.qdrant;

import java.util.List;
import java.util.Map;

public record EntryDetail(
        String id,
        String title,
        String type,
        String domain,
        double score,
        String submitted,
        String sourceDocumentId,
        String content,
        // frontmatter-enriched fields
        List<String> tags,
        Integer stalenessThreshold,
        String lastReviewed,
        String author,
        String verifiedOn,
        boolean verified,
        String constraints,
        String invalidationTriggers,
        String stalenessStatus,
        Map<String, Object> allFrontmatter) {
}
