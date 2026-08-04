package io.hortora.grove.analysis;

public record DuplicatePair(
        String entryA,
        String titleA,
        String sourceDocIdA,
        String entryB,
        String titleB,
        String sourceDocIdB,
        double similarity) {
}