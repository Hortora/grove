package io.hortora.grove.api;

import io.hortora.grove.health.IndexReconciler;
import io.hortora.grove.health.ReindexResult;
import io.hortora.grove.health.ReindexService;
import io.hortora.grove.health.ReconcileReport;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/api")
public class HealthResource {

    @Inject
    IndexReconciler reconciler;
    @Inject
    ReindexService reindexService;


    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GET
    @Path("/health/reconcile")
    @Produces(MediaType.APPLICATION_JSON)
    public ReconcileReport reconcile() {
        return reconciler.reconcile();
    }

    @POST
    @Path("/reindex")
    @Produces(MediaType.APPLICATION_JSON)
    public ReindexResult reindex() {
        return reindexService.triggerReindex();
    }
}
