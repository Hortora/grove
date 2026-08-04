package io.hortora.grove.api;

import io.hortora.grove.curation.CurationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/api/curation")
public class CurationResource {

    @Inject
    CurationService curationService;

    @POST
    @Path("/confirm/{sourceDocId:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> confirmFreshness(@PathParam("sourceDocId") String sourceDocId) {
        try {
            curationService.confirmFreshness(sourceDocId);
            return Map.of("status", "ok", "action", "confirmed", "entry", sourceDocId);
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @POST
    @Path("/retire/{sourceDocId:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> retire(@PathParam("sourceDocId") String sourceDocId, Map<String, String> body) {
        try {
            String reason = body != null ? body.getOrDefault("reason", "No reason given") : "No reason given";
            curationService.retire(sourceDocId, reason);
            return Map.of("status", "ok", "action", "retired", "entry", sourceDocId);
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @PUT
    @Path("/edit/{sourceDocId:.+}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> editEntry(@PathParam("sourceDocId") String sourceDocId, String updatedContent) {
        try {
            curationService.editEntry(sourceDocId, updatedContent);
            return Map.of("status", "ok", "action", "edited", "entry", sourceDocId);
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
