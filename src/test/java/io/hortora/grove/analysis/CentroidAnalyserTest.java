package io.hortora.grove.analysis;

import io.hortora.grove.qdrant.VectorEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CentroidAnalyserTest {

    @Test
    void centroidIsAverageOfVectors() {
        var entries = List.of(
                new VectorEntry("1", "A", "a.md", new float[]{2.0f, 0.0f}),
                new VectorEntry("2", "B", "b.md", new float[]{0.0f, 4.0f})
        );
        float[] centroid = CentroidAnalyser.computeCentroid(entries);
        assertEquals(1.0f, centroid[0], 0.0001);
        assertEquals(2.0f, centroid[1], 0.0001);
    }

    @Test
    void emptyListReturnEmptyCentroid() {
        float[] centroid = CentroidAnalyser.computeCentroid(List.of());
        assertEquals(0, centroid.length);
    }

    @Test
    void singleEntryReturnsSameVector() {
        var entries = List.of(
                new VectorEntry("1", "A", "a.md", new float[]{3.0f, 5.0f})
        );
        float[] centroid = CentroidAnalyser.computeCentroid(entries);
        assertEquals(3.0f, centroid[0], 0.0001);
        assertEquals(5.0f, centroid[1], 0.0001);
    }
}
