# CLAUDE.md

## Project Type

**Type:** java
**Stage:** pre-release

## Repository Purpose

**grove** — Garden analytics and curation dashboard. A standalone Quarkus web application that provides visual analytics over the Hortora knowledge garden corpus — domain density, staleness drift, retrieval effectiveness, vector-based quality signals — plus full curation actions (edit, retire, bulk operations, version lifecycle management).

Reads from Qdrant (vectors + content + metadata), SQLite databases (retrieval tracking, garden.db), and the garden git repo (mutations). Uses casehub pages/block-ui for the frontend.

## Stack

- **Quarkus 3.36.x** — runtime
- **casehub pages/block-ui** — frontend framework (LitElement, Lit 3.x)
- **Qdrant Java client** — garden data (vectors, payloads, similarity queries)
- **SQLite JDBC** — retrieval-tracking.db (usage frequency), garden.db (entry index, checked pairs)
- **JGit** — garden repo mutations (edit, retire, commit)
- **quarkus-quinoa** — frontend bundling (esbuild, yarn)
- **Java 25**

## Data Sources

| Source | Path | Access |
|--------|------|--------|
| Qdrant collection | `localhost:6333/collections/hortora_garden` | REST API (payloads + dense vectors) |
| Retrieval tracking | `~/.hortora/stats/retrieval-tracking.db` | SQLite read-only |
| Garden index | `~/.hortora/garden/garden.db` | SQLite read-only (entries, checked_pairs) |
| Garden entries | `~/.hortora/garden/` | Filesystem read (frontmatter), JGit write (mutations) |
| Version registry | `~/.hortora/garden/version-registry.yml` | YAML read/write (stack versions) |
| Grove analysis cache | `~/.hortora/grove.db` | SQLite read/write (duplicate pairs, analysis results) |
| Engine API | `localhost:8080` | REST (reindex trigger — requires engine#79) |

## Build

```bash
./mvnw verify                          # JVM tests
./mvnw quarkus:dev                     # dev mode (port 8090)
```

## Port

8090 (avoids conflict with engine on 8080)

## Key Design Decisions

- **Qdrant as primary data source** — Qdrant stores full entry content in the `content` payload field plus core metadata. Avoids needing the garden git repo for reads.
- **YAML frontmatter parsing** — Fields not in Qdrant payload (staleness_threshold, tags, last_reviewed, author, verified_on) are parsed from the content field at query time. Future: enrich Qdrant payload at ingestion time.
- **Garden git repo for mutations** — all curation actions (edit, retire, confirm freshness) write to the garden git repo and commit. Git is the source of truth for content.
- **Commit prefix** — all Grove mutations use `grove:` prefix in commit messages.

## Project Artifacts

| Path | What it is |
|------|------------|
| `CLAUDE.md` | Project conventions |
| `docs/specs/` | Design specs |
| `docs/plans/` | Implementation plans |

## Work Tracking

Issue tracking: enabled
GitHub repo: Hortora/grove
All commits reference an issue — `Refs #N` (ongoing) or `Closes #N` (done).
