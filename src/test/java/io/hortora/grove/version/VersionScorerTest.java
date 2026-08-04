package io.hortora.grove.version;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.hortora.grove.version.VersionScorer.Tier.*;
import static org.junit.jupiter.api.Assertions.*;

class VersionScorerTest {

    private static final Map<String, String> REGISTRY = Map.of(
            "quarkus", "3.36.1",
            "jdk", "25.0.2",
            "python", "3.14"
    );

    @Test
    void currentVersionScoresCurrent() {
        assertEquals(CURRENT, VersionScorer.score("quarkus 3.36.1", REGISTRY));
        assertEquals(CURRENT, VersionScorer.score("quarkus 3.35.0", REGISTRY));
    }

    @Test
    void twoMinorsBehindScoresAging() {
        assertEquals(AGING, VersionScorer.score("quarkus 3.34.0", REGISTRY));
        assertEquals(AGING, VersionScorer.score("quarkus 3.30.0", REGISTRY));
    }

    @Test
    void majorBehindScoresLegacy() {
        assertEquals(LEGACY, VersionScorer.score("quarkus 2.16.0", REGISTRY));
    }

    @Test
    void nullOrBlankReturnsUnknown() {
        assertEquals(UNKNOWN, VersionScorer.score(null, REGISTRY));
        assertEquals(UNKNOWN, VersionScorer.score("", REGISTRY));
        assertEquals(UNKNOWN, VersionScorer.score("  ", REGISTRY));
    }

    @Test
    void emptyRegistryReturnsUnknown() {
        assertEquals(UNKNOWN, VersionScorer.score("quarkus 3.36.1", Map.of()));
    }

    @Test
    void bareVersionMatchesFirstRegistryEntry() {
        assertEquals(CURRENT, VersionScorer.score("3.36.0", Map.of("quarkus", "3.36.1")));
    }

    @Test
    void jdkMajorVersionCompare() {
        assertEquals(CURRENT, VersionScorer.score("jdk 25", REGISTRY));
        assertEquals(LEGACY, VersionScorer.score("jdk 21", REGISTRY));
    }

    @Test
    void compareVersionsDirectly() {
        assertEquals(CURRENT, VersionScorer.compareVersions("3.36.1", "3.36.1"));
        assertEquals(AGING, VersionScorer.compareVersions("3.34.0", "3.36.1"));
        assertEquals(LEGACY, VersionScorer.compareVersions("2.0.0", "3.36.1"));
    }
}
