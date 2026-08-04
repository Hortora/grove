package io.hortora.grove.tracking;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RetrievalStatsServiceTest {

    private static final String DB_PATH = System.getProperty("user.home") + "/.hortora/stats/retrieval-tracking.db";

    @Test
    void getRetrievalCountsReturnsNonEmptyMap() {
        var service = new RetrievalStatsService(DB_PATH);
        Map<String, Integer> counts = service.getRetrievalCounts();

        assertFalse(counts.isEmpty());
        counts.values().forEach(c -> assertTrue(c > 0));
    }

    @Test
    void getAllStatsReturnsEntryRetrievalStats() {
        var service = new RetrievalStatsService(DB_PATH);
        List<EntryRetrievalStats> stats = service.getAllStats();

        assertFalse(stats.isEmpty());
        EntryRetrievalStats top = stats.get(0);
        assertNotNull(top.sourceDocumentId());
        assertTrue(top.retrievalCount() > 0);
        assertNotNull(top.lastRetrievedAt());
    }

    @Test
    void getNeverRetrievedFindsEntriesNotInDb() {
        var service = new RetrievalStatsService(DB_PATH);
        Set<String> allIds = Set.of(
                "jvm/GE-20260516-3a27dc.md",
                "fake/GE-NONEXISTENT-000000.md");

        Set<String> neverRetrieved = service.getNeverRetrieved(allIds);

        assertTrue(neverRetrieved.contains("fake/GE-NONEXISTENT-000000.md"));
    }

    @Test
    void retrievalCountsKeysMatchSourceDocumentIdFormat() {
        var service = new RetrievalStatsService(DB_PATH);
        Map<String, Integer> counts = service.getRetrievalCounts();

        counts.keySet().stream().limit(5).forEach(key ->
                assertTrue(key.endsWith(".md"), "Expected .md suffix but got: " + key));
    }
}
