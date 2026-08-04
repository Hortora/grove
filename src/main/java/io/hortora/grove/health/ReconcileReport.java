package io.hortora.grove.health;

import java.util.List;

public record ReconcileReport(
        int qdrantCount,
        int gardenDbCount,
        int fileCount,
        List<String> missingFromQdrant,
        List<String> missingFromDb) {
}
