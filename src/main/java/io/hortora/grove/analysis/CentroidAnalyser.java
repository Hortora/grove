package io.hortora.grove.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.hortora.grove.qdrant.QdrantGardenClient;
import io.hortora.grove.qdrant.VectorEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CentroidAnalyser {

    @Inject
    QdrantGardenClient client;

    public List<OutlierEntry> findOutliers(String domain) {
        List<VectorEntry> entries = client.fetchEntriesWithVectors(domain);
        if (entries.size() < 2) return List.of();

        float[] centroid = computeCentroid(entries);
        List<OutlierEntry> outliers = new ArrayList<>();

        for (VectorEntry e : entries) {
            double distance = 1.0 - DuplicateDetector.cosineSimilarity(e.vector(), centroid);
            outliers.add(new OutlierEntry(e.id(), e.title(), e.sourceDocumentId(), distance));
        }

        outliers.sort((a, b) -> Double.compare(b.distanceFromCentroid(), a.distanceFromCentroid()));
        return outliers;
    }

    public List<CrossDomainCandidate> findCrossDomainCandidates() {
        List<String> domains = client.getDomainStats().stream()
                .map(d -> d.domain())
                .toList();

        Map<String, float[]> centroids = new HashMap<>();
        Map<String, List<VectorEntry>> domainEntries = new HashMap<>();

        for (String domain : domains) {
            List<VectorEntry> entries = client.fetchEntriesWithVectors(domain);
            if (entries.size() < 2) continue;
            centroids.put(domain, computeCentroid(entries));
            domainEntries.put(domain, entries);
        }

        List<CrossDomainCandidate> candidates = new ArrayList<>();

        for (var entry : domainEntries.entrySet()) {
            String ownDomain = entry.getKey();
            float[] ownCentroid = centroids.get(ownDomain);

            for (VectorEntry ve : entry.getValue()) {
                double ownDistance = 1.0 - DuplicateDetector.cosineSimilarity(ve.vector(), ownCentroid);

                String closestDomain = null;
                double closestDistance = ownDistance;

                for (var other : centroids.entrySet()) {
                    if (other.getKey().equals(ownDomain)) continue;
                    double dist = 1.0 - DuplicateDetector.cosineSimilarity(ve.vector(), other.getValue());
                    if (dist < closestDistance) {
                        closestDistance = dist;
                        closestDomain = other.getKey();
                    }
                }

                if (closestDomain != null) {
                    candidates.add(new CrossDomainCandidate(
                            ve.id(), ve.title(), ve.sourceDocumentId(),
                            ownDomain, closestDomain,
                            ownDistance, closestDistance,
                            ownDistance - closestDistance));
                }
            }
        }

        candidates.sort((a, b) -> Double.compare(b.delta(), a.delta()));
        return candidates;
    }

    static float[] computeCentroid(List<VectorEntry> entries) {
        if (entries.isEmpty()) return new float[0];
        int dim = entries.getFirst().vector().length;
        float[] centroid = new float[dim];
        for (VectorEntry e : entries) {
            float[] v = e.vector();
            for (int i = 0; i < dim; i++) {
                centroid[i] += v[i];
            }
        }
        for (int i = 0; i < dim; i++) {
            centroid[i] /= entries.size();
        }
        return centroid;
    }
}