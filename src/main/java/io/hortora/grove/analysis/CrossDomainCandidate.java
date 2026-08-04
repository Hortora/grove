package io.hortora.grove.analysis;

public record CrossDomainCandidate(
        String entryId,
        String title,
        String sourceDocumentId,
        String currentDomain,
        String suggestedDomain,
        double ownDistance,
        double suggestedDistance,
        double delta) {
}