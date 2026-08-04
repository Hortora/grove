package io.hortora.grove.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisCacheStoreTest {

    private AnalysisCacheStore store;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        store = new AnalysisCacheStore(tempDir.resolve("grove.db").toString());
        store.init();
    }

    @Test
    void roundTrip() {
        store.cache("duplicates", "quarkus", "[{\"a\":1}]", 5);
        CacheEntry entry = store.getCached("duplicates", "quarkus");
        assertNotNull(entry);
        assertEquals("[{\"a\":1}]", entry.resultJson());
        assertEquals(5, entry.entryCount());
        assertNotNull(entry.analysedAt());
    }

    @Test
    void replaceOverwritesPrevious() {
        store.cache("outliers", "java", "[1]", 1);
        store.cache("outliers", "java", "[2]", 2);
        CacheEntry entry = store.getCached("outliers", "java");
        assertEquals("[2]", entry.resultJson());
        assertEquals(2, entry.entryCount());
    }

    @Test
    void missReturnsNull() {
        assertNull(store.getCached("coverage", "nonexistent"));
    }

    @Test
    void clearDomainRemovesAllTypesForDomain() {
        store.cache("duplicates", "quarkus", "[]", 0);
        store.cache("outliers", "quarkus", "[]", 0);
        store.cache("coverage", "quarkus", "{}", 0);
        store.cache("duplicates", "java", "[]", 0);

        store.clearDomain("quarkus");

        assertNull(store.getCached("duplicates", "quarkus"));
        assertNull(store.getCached("outliers", "quarkus"));
        assertNull(store.getCached("coverage", "quarkus"));
        assertNotNull(store.getCached("duplicates", "java"));
    }

    @Test
    void clearDomainAlsoInvalidatesCrossDomain() {
        store.cache("cross-domain", "__all__", "[]", 3);
        store.cache("duplicates", "quarkus", "[]", 0);

        store.clearDomain("quarkus");

        assertNull(store.getCached("cross-domain", "__all__"));
    }

    @Test
    void clearAllRemovesEverything() {
        store.cache("duplicates", "quarkus", "[]", 0);
        store.cache("outliers", "java", "[]", 0);

        store.clearAll();

        assertNull(store.getCached("duplicates", "quarkus"));
        assertNull(store.getCached("outliers", "java"));
    }

    @Test
    void schemaCreationIsIdempotent() {
        store.init();
        store.cache("duplicates", "test", "[]", 0);
        assertNotNull(store.getCached("duplicates", "test"));
    }

    @Test
    void differentTypesForSameDomainAreIndependent() {
        store.cache("duplicates", "quarkus", "[\"dup\"]", 1);
        store.cache("outliers", "quarkus", "[\"out\"]", 2);

        assertEquals("[\"dup\"]", store.getCached("duplicates", "quarkus").resultJson());
        assertEquals("[\"out\"]", store.getCached("outliers", "quarkus").resultJson());
    }
}
