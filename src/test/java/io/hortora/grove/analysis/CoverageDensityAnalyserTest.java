package io.hortora.grove.analysis;

import io.hortora.grove.qdrant.VectorEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoverageDensityAnalyserTest {

    @Test
    void twoCloseEntriesFormOneCluster() {
        var entries = List.of(
                new VectorEntry("1", "A", "a.md", new float[]{1.0f, 0.0f, 0.0f}),
                new VectorEntry("2", "B", "b.md", new float[]{0.99f, 0.1f, 0.0f})
        );
        double[][] dist = CoverageDensityAnalyser.computeDistanceMatrix(entries);
        int[] labels = CoverageDensityAnalyser.dbscan(dist, 0.3, 2);
        assertEquals(0, labels[0]);
        assertEquals(0, labels[1]);
    }

    @Test
    void twoDistantEntriesAreNoise() {
        var entries = List.of(
                new VectorEntry("1", "A", "a.md", new float[]{1.0f, 0.0f, 0.0f}),
                new VectorEntry("2", "B", "b.md", new float[]{0.0f, 1.0f, 0.0f})
        );
        double[][] dist = CoverageDensityAnalyser.computeDistanceMatrix(entries);
        int[] labels = CoverageDensityAnalyser.dbscan(dist, 0.3, 2);
        assertEquals(-1, labels[0]);
        assertEquals(-1, labels[1]);
    }

    @Test
    void threeEntriesTwoClustersOneSeparated() {
        var entries = List.of(
                new VectorEntry("1", "A", "a.md", new float[]{1.0f, 0.0f, 0.0f}),
                new VectorEntry("2", "B", "b.md", new float[]{0.99f, 0.05f, 0.0f}),
                new VectorEntry("3", "C", "c.md", new float[]{0.0f, 1.0f, 0.0f})
        );
        double[][] dist = CoverageDensityAnalyser.computeDistanceMatrix(entries);
        int[] labels = CoverageDensityAnalyser.dbscan(dist, 0.3, 2);
        assertEquals(labels[0], labels[1]);
        assertEquals(-1, labels[2]);
    }
}
