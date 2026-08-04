package io.hortora.grove.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.hortora.grove.config.GroveConfig;
import io.hortora.grove.qdrant.EntryDetail;
import io.hortora.grove.qdrant.FrontmatterParser;
import io.hortora.grove.qdrant.GardenEntry;
import io.hortora.grove.qdrant.QdrantGardenClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@jakarta.ws.rs.Path("/api")
public class EntryResource {

    @Inject
    QdrantGardenClient client;

    @Inject
    GroveConfig config;

    @GET
    @jakarta.ws.rs.Path("/entries/{geId}")
    @jakarta.ws.rs.Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public EntryDetail getEntry(@PathParam("geId") String geId) {
        List<GardenEntry> all = client.fetchAllEntries();
        GardenEntry entry = all.stream()
                .filter(e -> e.sourceDocumentId() != null && e.sourceDocumentId().contains(geId))
                .findFirst()
                .orElseThrow(() -> new WebApplicationException("Entry not found: " + geId, Response.Status.NOT_FOUND));

        return enrichWithFrontmatter(entry);
    }

    @GET
    @jakarta.ws.rs.Path("/domains/{domain}/entries")
    @jakarta.ws.rs.Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<EntryDetail> getDomainEntries(
            @PathParam("domain") String domain,
            @QueryParam("sort") @DefaultValue("score") String sort,
            @QueryParam("type") String typeFilter,
            @QueryParam("stale") String staleFilter) {

        List<GardenEntry> all = client.fetchAllEntries();
        var filtered = all.stream()
                .filter(e -> domain.equals(e.domain()));

        if (typeFilter != null && !typeFilter.isEmpty()) {
            filtered = filtered.filter(e -> typeFilter.equals(e.type()));
        }

        List<EntryDetail> enriched = filtered
                .map(this::enrichWithFrontmatter)
                .toList();

        if (staleFilter != null) {
            boolean wantStale = Boolean.parseBoolean(staleFilter);
            enriched = enriched.stream()
                    .filter(e -> "stale".equals(e.stalenessStatus()) == wantStale)
                    .toList();
        }

        Comparator<EntryDetail> comparator = switch (sort) {
            case "submitted" -> Comparator.comparing(EntryDetail::submitted, Comparator.nullsLast(Comparator.reverseOrder()));
            case "staleness" -> Comparator.comparing(EntryDetail::stalenessStatus, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparingDouble(EntryDetail::score).reversed();
        };

        return enriched.stream().sorted(comparator).toList();
    }

    @SuppressWarnings("unchecked")
    private EntryDetail enrichWithFrontmatter(GardenEntry entry) {
        Map<String, Object> frontmatter = Map.of();
        String fileContent = null;

        if (entry.sourceDocumentId() != null) {
            Path filePath = Path.of(config.garden().path(), entry.sourceDocumentId());
            try {
                if (Files.exists(filePath)) {
                    String raw = Files.readString(filePath);
                    frontmatter = FrontmatterParser.parse(raw);
                    fileContent = FrontmatterParser.bodyWithoutFrontmatter(raw);
                }
            } catch (IOException ignored) {
            }
        }

        List<String> tags = extractTags(frontmatter);
        Integer stalenessThreshold = frontmatter.containsKey("staleness_threshold")
                ? ((Number) frontmatter.get("staleness_threshold")).intValue() : null;
        String lastReviewed = stringOrNull(frontmatter, "last_reviewed");
        String author = stringOrNull(frontmatter, "author");
        String verifiedOn = stringOrNull(frontmatter, "verified_on");
        boolean verified = Boolean.TRUE.equals(frontmatter.get("verified"));
        String constraints = stringOrNull(frontmatter, "constraints");
        String invalidationTriggers = stringOrNull(frontmatter, "invalidation_triggers");

        String fmSubmitted = stringOrNull(frontmatter, "submitted");
        String stalenessStatus = computeStalenessStatus(fmSubmitted != null ? fmSubmitted : entry.submitted(), lastReviewed, stalenessThreshold);

        return new EntryDetail(
                entry.id(), entry.title(), entry.type(), entry.domain(),
                entry.score(), entry.submitted(), entry.sourceDocumentId(),
                fileContent != null ? fileContent : entry.content(),
                tags, stalenessThreshold, lastReviewed, author, verifiedOn,
                verified, constraints, invalidationTriggers,
                stalenessStatus, frontmatter);
    }

    static String computeStalenessStatus(String submitted, String lastReviewed, Integer thresholdDays) {
        if (thresholdDays == null) return "unknown";

        try {
            String dateStr = lastReviewed != null ? lastReviewed : submitted;
            if (dateStr == null) return "unknown";
            LocalDate referenceDate = LocalDate.parse(dateStr);
            long daysSince = ChronoUnit.DAYS.between(referenceDate, LocalDate.now());
            if (daysSince > thresholdDays) return "stale";
            if (daysSince > thresholdDays * 0.8) return "aging";
            return "current";
        } catch (Exception e) {
            return "unknown";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTags(Map<String, Object> frontmatter) {
        Object raw = frontmatter.get("tags");
        if (raw == null) return List.of();
        if (raw instanceof List) return (List<String>) raw;
        return List.of(raw.toString());
    }

    private String stringOrNull(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}
