package io.hortora.grove.qdrant;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QdrantGardenClientTest {

    private QdrantGardenClient client;

    @BeforeEach
    void setUp() {
        client = new QdrantGardenClient("localhost", 6333, "test");
    }

    @Test
    void computeDomainStatsGroupsByDomainWithCounts() {
        var entries = List.of(
                entry("1", "Entry A", "gotcha", "jvm", 12),
                entry("2", "Entry B", "technique", "jvm", 10),
                entry("3", "Entry C", "gotcha", "jvm", 8),
                entry("4", "Entry D", "gotcha", "tools", 14),
                entry("5", "Entry E", "undocumented", "tools", 9));

        List<DomainStats> stats = client.computeDomainStats(entries);

        assertEquals(2, stats.size());

        DomainStats jvm = stats.stream().filter(s -> s.domain().equals("jvm")).findFirst().orElseThrow();
        assertEquals(3, jvm.entryCount());
        assertEquals(Map.of("gotcha", 2, "technique", 1), jvm.typeBreakdown());
        assertEquals(10.0, jvm.averageScore(), 0.01);

        DomainStats tools = stats.stream().filter(s -> s.domain().equals("tools")).findFirst().orElseThrow();
        assertEquals(2, tools.entryCount());
        assertEquals(Map.of("gotcha", 1, "undocumented", 1), tools.typeBreakdown());
        assertEquals(11.5, tools.averageScore(), 0.01);
    }

    @Test
    void computeDomainStatsSortsByEntryCountDescending() {
        var entries = List.of(
                entry("1", "A", "gotcha", "small", 5),
                entry("2", "B", "gotcha", "big", 5),
                entry("3", "C", "gotcha", "big", 5),
                entry("4", "D", "gotcha", "big", 5));

        List<DomainStats> stats = client.computeDomainStats(entries);

        assertEquals("big", stats.get(0).domain());
        assertEquals("small", stats.get(1).domain());
    }

    @Test
    void computeDomainStatsSkipsNullDomain() {
        var entries = List.of(
                entry("1", "A", "gotcha", null, 5),
                entry("2", "B", "gotcha", "jvm", 10));

        List<DomainStats> stats = client.computeDomainStats(entries);

        assertEquals(1, stats.size());
        assertEquals("jvm", stats.get(0).domain());
    }

    @Test
    void computeOverviewCountsTotals() {
        var entries = List.of(
                entry("1", "A", "gotcha", "jvm", 12),
                entry("2", "B", "technique", "jvm", 10),
                entry("3", "C", "gotcha", "tools", 8));
        var domainStats = client.computeDomainStats(entries);

        GardenOverview overview = client.computeOverview(entries, domainStats);

        assertEquals(3, overview.totalEntries());
        assertEquals(2, overview.totalDomains());
    }

    @Test
    void computeDomainStatsHandlesEmptyList() {
        List<DomainStats> stats = client.computeDomainStats(List.of());
        assertTrue(stats.isEmpty());
    }

    @Test
    void computeDomainStatsHandlesZeroScore() {
        var entries = List.of(
                entry("1", "A", "gotcha", "jvm", 0));

        List<DomainStats> stats = client.computeDomainStats(entries);

        assertEquals(0.0, stats.get(0).averageScore(), 0.01);
    }

    private GardenEntry entry(String id, String title, String type, String domain, double score) {
        return new GardenEntry(id, title, type, domain, score, "2026-05-16", "doc/" + id + ".md", null);
    }
}
