package io.hortora.grove.qdrant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.hortora.grove.config.GroveConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class QdrantGardenClient {

    private final String baseUrl;
    private final String collection;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    @Inject
    public QdrantGardenClient(GroveConfig config) {
        this(config.qdrant().host(), config.qdrant().port(), config.qdrant().collection());
    }

    public QdrantGardenClient(String host, int port, String collection) {
        this.baseUrl = "http://" + host + ":" + port;
        this.collection = collection;
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    public List<GardenEntry> fetchAllEntries() {
        var entries = new ArrayList<GardenEntry>();
        String offset = null;

        try {
            while (true) {
                ObjectNode body = mapper.createObjectNode();
                body.put("limit", 100);
                body.put("with_vector", false);
                body.putObject("with_payload")
                        .putArray("include")
                        .add("title").add("type").add("domain")
                        .add("score").add("submitted").add("sourceDocumentId");
                if (offset != null) {
                    body.put("offset", offset);
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/collections/" + collection + "/points/scroll"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode result = mapper.readTree(response.body()).get("result");
                JsonNode points = result.get("points");

                for (JsonNode point : points) {
                    entries.add(parseEntry(point));
                }

                JsonNode nextOffset = result.get("next_page_offset");
                if (nextOffset == null || nextOffset.isNull()) {
                    break;
                }
                offset = nextOffset.asText();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch entries from Qdrant", e);
        }

        return entries;
    }

    private GardenEntry parseEntry(JsonNode point) {
        JsonNode payload = point.get("payload");
        String id = point.get("id").asText();
        String scoreStr = payload.has("score") ? payload.get("score").asText() : "0";
        double score;
        try {
            score = Double.parseDouble(scoreStr);
        } catch (NumberFormatException e) {
            score = 0;
        }

        return new GardenEntry(
                id,
                textOrNull(payload, "title"),
                textOrNull(payload, "type"),
                textOrNull(payload, "domain"),
                score,
                textOrNull(payload, "submitted"),
                textOrNull(payload, "sourceDocumentId"),
                null);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    public List<DomainStats> computeDomainStats(List<GardenEntry> entries) {
        Map<String, List<GardenEntry>> byDomain = entries.stream()
                .filter(e -> e.domain() != null)
                .collect(Collectors.groupingBy(GardenEntry::domain, LinkedHashMap::new, Collectors.toList()));

        return byDomain.entrySet().stream()
                .map(e -> buildDomainStats(e.getKey(), e.getValue()))
                .sorted((a, b) -> Integer.compare(b.entryCount(), a.entryCount()))
                .toList();
    }

    private DomainStats buildDomainStats(String domain, List<GardenEntry> entries) {
        Map<String, Integer> typeBreakdown = entries.stream()
                .filter(e -> e.type() != null)
                .collect(Collectors.groupingBy(GardenEntry::type, Collectors.summingInt(e -> 1)));

        double avgScore = entries.stream()
                .mapToDouble(GardenEntry::score)
                .average()
                .orElse(0);

        return new DomainStats(domain, entries.size(), typeBreakdown, avgScore, 0, 0);
    }

    public GardenOverview computeOverview(List<GardenEntry> entries, List<DomainStats> domainStats) {
        int untaggedCount = 0;
        return new GardenOverview(
                entries.size(),
                domainStats.size(),
                0,
                untaggedCount,
                0);
    }

    public List<DomainStats> getDomainStats() {
        return computeDomainStats(fetchAllEntries());
    }

    public GardenOverview getOverview() {
        List<GardenEntry> entries = fetchAllEntries();
        List<DomainStats> stats = computeDomainStats(entries);
        return computeOverview(entries, stats);
    }
}
