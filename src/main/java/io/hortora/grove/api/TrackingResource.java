package io.hortora.grove.api;

import java.util.List;

import io.hortora.grove.tracking.EntryRetrievalStats;
import io.hortora.grove.tracking.RetrievalStatsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/tracking")
public class TrackingResource {

    @Inject
    RetrievalStatsService statsService;

    @GET
    @Path("/stats")
    @Produces(MediaType.APPLICATION_JSON)
    public List<EntryRetrievalStats> getStats() {
        return statsService.getAllStats();
    }
}
