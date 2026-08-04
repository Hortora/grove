package io.hortora.grove.api;

import java.util.List;

import java.util.Map;

import io.hortora.grove.qdrant.DomainStats;
import io.hortora.grove.qdrant.GardenEntry;
import io.hortora.grove.qdrant.GardenOverview;
import io.hortora.grove.qdrant.QdrantGardenClient;
import io.hortora.grove.tracking.RetrievalStatsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public class DomainResource {

    @Inject
    QdrantGardenClient client;

    @Inject
    RetrievalStatsService retrievalStats;

    @GET
    @Path("/domains")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainStats> getDomains() {
        List<GardenEntry> entries = client.fetchAllEntries();
        Map<String, Integer> counts = retrievalStats.getRetrievalCounts();
        return client.computeDomainStats(entries, counts);
    }

    @GET
    @Path("/overview")
    @Produces(MediaType.APPLICATION_JSON)
    public GardenOverview getOverview() {
        List<GardenEntry> entries = client.fetchAllEntries();
        Map<String, Integer> counts = retrievalStats.getRetrievalCounts();
        List<DomainStats> stats = client.computeDomainStats(entries, counts);
        return client.computeOverview(entries, stats);
    }
}
