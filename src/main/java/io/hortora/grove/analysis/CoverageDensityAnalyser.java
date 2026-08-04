package io.hortora.grove.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.hortora.grove.qdrant.QdrantGardenClient;
import io.hortora.grove.qdrant.VectorEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CoverageDensityAnalyser {

    private static final double EPS = 0.3;
    private static final int MIN_PTS = 2;

    @Inject
    QdrantGardenClient client;

    public CoverageResult analyse(String domain) {
        List<VectorEntry> entries = client.fetchEntriesWithVectors(domain);
        if (entries.size() < MIN_PTS) {
            return new CoverageResult(domain, entries.size(), 0, 0.0, List.of());
        }

        double[][] distances = computeDistanceMatrix(entries);
        int[] labels = dbscan(distances, EPS, MIN_PTS);

        int maxLabel = Arrays.stream(labels).max().orElse(-1);
        int clusterCount = maxLabel + 1;

        List<CoverageResult.ClusterInfo> clusters = new ArrayList<>();
        for (int c = 0; c <= maxLabel; c++) {
            List<String> ids = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] == c) {
                    ids.add(entries.get(i).id());
                    titles.add(entries.get(i).title());
                }
            }
            clusters.add(new CoverageResult.ClusterInfo(c, ids.size(), ids, titles));
        }

        int noise = (int) Arrays.stream(labels).filter(l -> l == -1).count();
        double spread = clusterCount == 0 ? 0 : computeSpread(distances, labels, clusterCount);

        return new CoverageResult(domain, entries.size(), clusterCount, spread, clusters);
    }

    static double[][] computeDistanceMatrix(List<VectorEntry> entries) {
        int n = entries.size();
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double d = 1.0 - DuplicateDetector.cosineSimilarity(entries.get(i).vector(), entries.get(j).vector());
                dist[i][j] = d;
                dist[j][i] = d;
            }
        }
        return dist;
    }

    static int[] dbscan(double[][] distances, double eps, int minPts) {
        int n = distances.length;
        int[] labels = new int[n];
        Arrays.fill(labels, -2);
        int clusterId = -1;

        for (int i = 0; i < n; i++) {
            if (labels[i] != -2) continue;

            List<Integer> neighbors = rangeQuery(distances, i, eps);
            if (neighbors.size() < minPts) {
                labels[i] = -1;
                continue;
            }

            clusterId++;
            labels[i] = clusterId;
            List<Integer> seed = new ArrayList<>(neighbors);
            seed.remove(Integer.valueOf(i));

            for (int si = 0; si < seed.size(); si++) {
                int q = seed.get(si);
                if (labels[q] == -1) labels[q] = clusterId;
                if (labels[q] != -2) continue;
                labels[q] = clusterId;

                List<Integer> qNeighbors = rangeQuery(distances, q, eps);
                if (qNeighbors.size() >= minPts) {
                    for (int nb : qNeighbors) {
                        if (!seed.contains(nb)) seed.add(nb);
                    }
                }
            }
        }
        return labels;
    }

    private static List<Integer> rangeQuery(double[][] distances, int p, double eps) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            if (distances[p][i] <= eps) result.add(i);
        }
        return result;
    }

    private double computeSpread(double[][] distances, int[] labels, int clusterCount) {
        double totalIntra = 0;
        int pairCount = 0;
        for (int c = 0; c < clusterCount; c++) {
            List<Integer> members = new ArrayList<>();
            for (int i = 0; i < labels.length; i++) {
                if (labels[i] == c) members.add(i);
            }
            for (int i = 0; i < members.size(); i++) {
                for (int j = i + 1; j < members.size(); j++) {
                    totalIntra += distances[members.get(i)][members.get(j)];
                    pairCount++;
                }
            }
        }
        return pairCount > 0 ? totalIntra / pairCount : 0;
    }
}