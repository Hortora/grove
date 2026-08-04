package io.hortora.grove.analysis;

import java.util.List;
import java.util.Map;

public record CoverageResult(
        String domain,
        int entryCount,
        int clusterCount,
        double spreadMetric,
        List<ClusterInfo> clusters) {

    public record ClusterInfo(int id, int size, List<String> entryIds, List<String> titles) {
    }
}
