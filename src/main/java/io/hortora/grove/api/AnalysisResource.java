package io.hortora.grove.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hortora.grove.analysis.AnalysisCacheStore;
import io.hortora.grove.analysis.CacheEntry;
import io.hortora.grove.analysis.CentroidAnalyser;
import io.hortora.grove.analysis.CoverageDensityAnalyser;
import io.hortora.grove.analysis.CoverageResult;
import io.hortora.grove.analysis.CrossDomainCandidate;
import io.hortora.grove.analysis.DuplicateDetector;
import io.hortora.grove.analysis.DuplicatePair;
import io.hortora.grove.analysis.OutlierEntry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/api/analysis")
public class AnalysisResource {

    private static final Logger LOG = Logger.getLogger(AnalysisResource.class.getName());

    @Inject
    DuplicateDetector       duplicateDetector;
    @Inject
    CentroidAnalyser        centroidAnalyser;
    @Inject
    CoverageDensityAnalyser coverageAnalyser;
    @Inject
    AnalysisCacheStore      cacheStore;
    @Inject
    ObjectMapper            mapper;

    // --- Duplicates ---

    @POST
    @Path("/duplicates/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> analyseDuplicates(@PathParam("domain") String domain) {
        try {
            List<DuplicatePair> pairs = duplicateDetector.analyse(domain);
            cacheQuietly("duplicates", domain, pairs, pairs.size());
            return resultMap(domain, "pairs", pairs, pairs.size());
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/duplicates/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getCachedDuplicates(@PathParam("domain") String domain) {
        return cachedResultMap("duplicates", domain, "pairs",
                               new TypeReference<List<DuplicatePair>>() {}, List.of());
    }

    // --- Outliers ---

    @POST
    @Path("/outliers/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> analyseOutliers(@PathParam("domain") String domain) {
        try {
            List<OutlierEntry> outliers = centroidAnalyser.findOutliers(domain);
            cacheQuietly("outliers", domain, outliers, outliers.size());
            return resultMap(domain, "entries", outliers, outliers.size());
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/outliers/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getCachedOutliers(@PathParam("domain") String domain) {
        return cachedResultMap("outliers", domain, "entries",
                               new TypeReference<List<OutlierEntry>>() {}, List.of());
    }

    // --- Cross-domain ---

    @POST
    @Path("/cross-domain")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> analyseCrossDomain() {
        try {
            List<CrossDomainCandidate> candidates = centroidAnalyser.findCrossDomainCandidates();
            cacheQuietly("cross-domain", "__all__", candidates, candidates.size());
            return resultMap(null, "candidates", candidates, candidates.size());
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/cross-domain")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getCachedCrossDomain() {
        return cachedResultMap("cross-domain", "__all__", "candidates",
                               new TypeReference<List<CrossDomainCandidate>>() {}, List.of());
    }

    // --- Coverage ---

    @POST
    @Path("/coverage/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> analyseCoverage(@PathParam("domain") String domain) {
        try {
            CoverageResult result = coverageAnalyser.analyse(domain);
            cacheQuietly("coverage", domain, result, result.entryCount());
            return resultMap(domain, "result", result, result.entryCount());
        } catch (Exception e) {
            throw new WebApplicationException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/coverage/{domain}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getCachedCoverage(@PathParam("domain") String domain) {
        return cachedResultMap("coverage", domain, "result",
                               new TypeReference<CoverageResult>() {}, null);
    }

    // --- Helpers ---

    private <T> void cacheQuietly(String type, String domain, T result, int count) {
        try {
            cacheStore.cache(type, domain, mapper.writeValueAsString(result), count);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Cache write failed for " + type + "/" + domain, e);
        }
    }

    private <T> Map<String, Object> cachedResultMap(String type, String domain,
                                                    String resultKey, TypeReference<T> typeRef, T emptyDefault) {
        CacheEntry          entry = cacheStore.getCached(type, domain);
        Map<String, Object> map   = new LinkedHashMap<>();
        if (!"__all__".equals(domain)) {
            map.put("domain", domain);
        }
        if (entry != null) {
            try {
                T result = mapper.readValue(entry.resultJson(), typeRef);
                map.put("count", entry.entryCount());
                map.put(resultKey, result);
                map.put("analysedAt", entry.analysedAt().toString());
            } catch (JsonProcessingException e) {
                map.put("count", 0);
                map.put(resultKey, emptyDefault);
                map.put("analysedAt", null);
            }
        } else {
            map.put("count", 0);
            map.put(resultKey, emptyDefault);
            map.put("analysedAt", null);
        }
        return map;
    }

    private Map<String, Object> resultMap(String domain, String resultKey,
                                          Object result, int count) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (domain != null) {
            map.put("domain", domain);
        }
        map.put("count", count);
        map.put(resultKey, result);
        map.put("analysedAt", Instant.now().toString());
        return map;
    }
}