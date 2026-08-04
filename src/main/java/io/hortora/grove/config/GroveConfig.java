package io.hortora.grove.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "grove")
public interface GroveConfig {

    Qdrant qdrant();

    Garden garden();

    RetrievalTracking retrievalTracking();

    GardenDb gardenDb();

    Engine engine();


    interface Qdrant {
        @WithDefault("localhost")
        String host();

        @WithDefault("6333")
        int port();

        @WithDefault("hortora_garden")
        String collection();
    }

    interface Garden {
        String path();
    }

    interface RetrievalTracking {
        String path();
    }

    interface GardenDb {
        String path();
    }

    interface Engine {
        @WithDefault("http://localhost:8080")
        String url();
    }

    interface AnalysisCache {
        @WithDefault("${user.home}/.hortora/grove.db")
        String path();
    }

    AnalysisCache analysisCache();

}
