package io.hortora.grove.api;

import io.hortora.grove.health.IndexReconciler;
import io.hortora.grove.health.ReconcileReport;
import io.hortora.grove.health.ReindexResult;
import io.hortora.grove.health.ReindexService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api")
public class HealthResource {

    @Inject
    IndexReconciler reconciler;
    @Inject
    ReindexService reindexService;
    @Inject
    io.hortora.grove.version.VersionRegistry versionRegistry;


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

    @GET
    @Path("/versions")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getVersions() {
        return versionRegistry.getVersions();
    }

    @PUT
    @Path("/versions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> updateVersions(Map<String, String> versions) {
        try {
            for (var entry : versions.entrySet()) {
                versionRegistry.setVersion(entry.getKey(), entry.getValue());
            }
            return versionRegistry.getVersions();
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
