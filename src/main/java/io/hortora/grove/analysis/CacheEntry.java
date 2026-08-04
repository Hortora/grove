package io.hortora.grove.analysis;

import java.time.Instant;

public record CacheEntry(String resultJson, int entryCount, Instant analysedAt) {}
