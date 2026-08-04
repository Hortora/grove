package io.hortora.grove.api;

import java.util.Map;

import io.hortora.grove.health.IndexReconciler;
import io.hortora.grove.health.ReconcileReport;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public class HealthResource {

    @Inject
    IndexReconciler reconciler;

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
}
