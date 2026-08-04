package io.hortora.grove.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateDetectorTest {

    @Test
    void identicalVectorsHaveSimilarityOne() {
        float[] v = {1.0f, 0.0f, 0.0f};
        assertEquals(1.0, DuplicateDetector.cosineSimilarity(v, v), 0.0001);
    }

    @Test
    void orthogonalVectorsHaveSimilarityZero() {
        float[] a = {1.0f, 0.0f, 0.0f};
        float[] b = {0.0f, 1.0f, 0.0f};
        assertEquals(0.0, DuplicateDetector.cosineSimilarity(a, b), 0.0001);
    }

    @Test
    void similarVectorsAboveThreshold() {
        float[] a = {1.0f, 0.0f, 0.0f};
        float[] b = {0.99f, 0.1f, 0.0f};
        double sim = DuplicateDetector.cosineSimilarity(a, b);
        assertTrue(sim > 0.92, "Expected > 0.92, got " + sim);
    }

    @Test
    void differentVectorsBelowThreshold() {
        float[] a = {1.0f, 0.0f, 0.0f};
        float[] b = {0.5f, 0.8f, 0.3f};
        double sim = DuplicateDetector.cosineSimilarity(a, b);
        assertTrue(sim < 0.92, "Expected < 0.92, got " + sim);
    }

    @Test
    void emptyVectorsReturnZero() {
        assertEquals(0.0, DuplicateDetector.cosineSimilarity(new float[0], new float[0]));
    }

    @Test
    void mismatchedLengthsReturnZero() {
        float[] a = {1.0f, 0.0f};
        float[] b = {1.0f, 0.0f, 0.0f};
        assertEquals(0.0, DuplicateDetector.cosineSimilarity(a, b));
    }
}
