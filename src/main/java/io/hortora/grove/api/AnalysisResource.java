package io.hortora.grove.api;

import java.util.List;
import java.util.Map;

import io.hortora.grove.analysis.DuplicateDetector;
import io.hortora.grove.analysis.DuplicatePair;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/analysis")
public class AnalysisResource {

    @Inject
    DuplicateDetector duplicateDetector;

    @POST
    @Path("/duplicates/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> analyseDuplicates(@PathParam("domain") String domain) {
        try {
            List<DuplicatePair> pairs = duplicateDetector.analyse(domain);
            return Map.of("status", "ok", "domain", domain, "count", pairs.size(), "pairs", pairs);
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/duplicates/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getCachedDuplicates(@PathParam("domain") String domain) {
        List<DuplicatePair> pairs = duplicateDetector.getCached(domain);
        return Map.of("domain", domain, "count", pairs.size(), "pairs", pairs);
    }
}