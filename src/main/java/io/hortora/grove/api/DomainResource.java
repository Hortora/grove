package io.hortora.grove.api;

import java.util.List;

import io.hortora.grove.qdrant.DomainStats;
import io.hortora.grove.qdrant.GardenOverview;
import io.hortora.grove.qdrant.QdrantGardenClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public class DomainResource {

    @Inject
    QdrantGardenClient client;

    @GET
    @Path("/domains")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DomainStats> getDomains() {
        return client.getDomainStats();
    }

    @GET
    @Path("/overview")
    @Produces(MediaType.APPLICATION_JSON)
    public GardenOverview getOverview() {
        return client.getOverview();
    }
}
