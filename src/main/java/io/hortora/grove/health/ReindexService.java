package io.hortora.grove.health;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import io.hortora.grove.config.GroveConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReindexService {

    @Inject
    GroveConfig config;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ReindexResult triggerReindex() {
        String url = config.engine().url() + "/api/garden/reindex";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return ReindexResult.success(response.body());
            } else {
                return ReindexResult.error("Engine returned " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            return ReindexResult.error("Engine unreachable at " + url + ": " + e.getMessage());
        }
    }
}