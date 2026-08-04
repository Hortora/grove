# grove.db Analysis Cache

**Date:** 2026-08-04
**Issue:** #22 (Hortora/grove)
**Status:** Design approved

---

## Problem

Four vector quality analysis operations (near-duplicates, semantic outliers, cross-domain similarity, coverage density) compute results from Qdrant vectors on every request. Only duplicates cache results in grove.db via a normalised `duplicate_pairs` table. The other three recompute every time.

The existing caching in DuplicateDetector also has structural problems: it mixes analysis logic with SQLite persistence, derives the grove.db path from `config.garden().path() + "/../grove.db"` instead of a dedicated config property, and uses a normalised table for what is fundamentally a key-value cache (we never query individual cached rows).

## Design

### grove.db as a key-value cache

grove.db stores computed analysis results. Every row is derived from Qdrant vectors, read as a batch, and can be rebuilt from scratch. A single table replaces the normalised `duplicate_pairs` table:

```sql
CREATE TABLE analysis_cache (
    analysis_type TEXT NOT NULL,
    domain        TEXT NOT NULL,
    result_json   TEXT NOT NULL,
    entry_count   INTEGER NOT NULL,
    analysed_at   TEXT NOT NULL,
    PRIMARY KEY (analysis_type, domain)
)
```

- Keyed by `(analysis_type, domain)` — four types: `duplicates`, `outliers`, `cross-domain`, `coverage`
- `result_json` — opaque JSON payload, serialised/deserialised by each analyser
- `entry_count` — allows dashboard tiles to show counts without deserialising:

  | Analysis type | `entry_count` meaning |
  |---------------|----------------------|
  | `duplicates` | Number of flagged pairs |
  | `outliers` | Number of entries in the domain (all ranked by distance) |
  | `cross-domain` | Number of miscategorisation candidates |
  | `coverage` | Number of entries in the domain |

- `analysed_at` — ISO-8601 timestamp for cache freshness
- `INSERT OR REPLACE` for writes — no separate delete + insert
- Cross-domain analysis uses sentinel domain `"__all__"`

### Configuration

Add `AnalysisCache` interface to `GroveConfig`:

```java
interface AnalysisCache {
    @WithDefault("${user.home}/.hortora/grove.db")
    String path();
}

AnalysisCache analysisCache();
```

Replaces the derived path in DuplicateDetector. Consistent with existing config pattern (Qdrant, garden, gardenDb, retrievalTracking all have explicit paths).

### GroveDb service

`io.hortora.grove.analysis.AnalysisCacheStore` — `@ApplicationScoped` CDI bean.

Responsibilities:
- Owns SQLite connection lifecycle (path from `GroveConfig.analysisCache().path()`)
- Creates `analysis_cache` table on first use
- Connections opened with WAL mode and 5-second busy timeout for concurrent access safety
- Four operations:

```java
void cache(String analysisType, String domain, String resultJson, int entryCount)
CacheEntry getCached(String analysisType, String domain)  // returns null on miss
void clearDomain(String domain)                           // removes all types for a domain + cross-domain
void clearAll()                                           // removes everything (full refresh)
```

`clearDomain` also removes the `("cross-domain", "__all__")` entry, since per-domain changes invalidate cross-domain results.

```java
record CacheEntry(String resultJson, int entryCount, Instant analysedAt) {}
```

Schema init also drops the legacy `duplicate_pairs` table if it exists (orphaned cache data from the old DuplicateDetector pattern).

What AnalysisCacheStore does NOT do:
- No JSON serialisation — stores/returns opaque strings
- No awareness of analysis types — generic `(type, domain) -> blob` cache
- No connection pooling — open/close per operation

### Analyser refactoring

Resource layer orchestrates caching. Analysers are pure computation:

- `DuplicateDetector.analyse(domain)` — computes and returns `List<DuplicatePair>`. All SQLite persistence code removed (ensureGroveDb, cacheResults, getCached methods). Retains garden.db access for checked pairs (different concern).
- `CentroidAnalyser.findOutliers(domain)` — unchanged, pure computation.
- `CentroidAnalyser.findCrossDomainCandidates()` — unchanged, pure computation.
- `CoverageDensityAnalyser.analyse(domain)` — unchanged, pure computation.

### API pattern

AnalysisResource injects AnalysisCacheStore and ObjectMapper alongside analysers. Uniform pattern:

| Method | Path | Action |
|--------|------|--------|
| POST | `/api/analysis/duplicates/{domain}` | Compute + cache + return |
| GET | `/api/analysis/duplicates/{domain}` | Serve from cache |
| POST | `/api/analysis/outliers/{domain}` | Compute + cache + return |
| GET | `/api/analysis/outliers/{domain}` | Serve from cache |
| POST | `/api/analysis/cross-domain` | Compute + cache + return |
| GET | `/api/analysis/cross-domain` | Serve from cache |
| POST | `/api/analysis/coverage/{domain}` | Compute + cache + return |
| GET | `/api/analysis/coverage/{domain}` | Serve from cache |

Response shape — existing field names preserved, `analysedAt` added:

```json
{"domain": "quarkus", "count": 3, "pairs": [...], "analysedAt": "2026-08-04T14:30:00Z"}
```

GET with no cache returns empty results with `analysedAt: null`. Frontend uses this to prompt "Run analysis?" rather than silently triggering expensive computation.

Breaking change: existing GET endpoints for outliers, cross-domain, coverage currently compute live. After this change they serve from cache (empty if never computed). Pre-release — acceptable.

**Cache write resilience:** POST endpoints must return computed results even if the cache write fails. Catch SQLite exceptions in the resource layer, log the failure, and still return the analysis results to the caller. The computation is expensive — losing it because of a cache write error is unacceptable.

**Issue #22 AC update:** The acceptance criterion "fall back to live computation on cache miss" is intentionally not implemented. GET returns empty with `analysedAt: null` instead. Silent fallback to expensive computation on every uncached page load contradicts the design spec's "on demand, not on every page load" principle. Update issue #22 AC to reflect this.

### Testing

**AnalysisCacheStore** — unit tests with temp-file SQLite:
- Round-trip: cache then retrieve, verify JSON and metadata
- Replace: cache same key twice, second overwrites first
- Miss: getCached for absent key returns null
- clearDomain: removes all analysis types for that domain
- Idempotent schema creation

**DuplicateDetector** — existing tests updated:
- Remove SQLite assertions (caching moved out)
- Keep analysis logic tests (cosine similarity, checked pair exclusion, threshold)

**CentroidAnalyser / CoverageDensityAnalyser** — existing tests unchanged (pure computation).

**AnalysisResource** — integration tests:
- POST computes, caches, returns results with analysedAt
- GET after POST returns cached results
- GET with no prior POST returns empty with analysedAt null
- POST again overwrites previous cache

## Out of scope

- Response format standardisation (field names vary: `pairs`, `entries`, `candidates`)
- Frontend changes (separate commits)
- Connection pooling
- Computation timeouts (analyser concern, not cache layer)
