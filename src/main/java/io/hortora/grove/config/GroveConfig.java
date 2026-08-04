package io.hortora.grove.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "grove")
public interface GroveConfig {

    Qdrant qdrant();

    Garden garden();

    RetrievalTracking retrievalTracking();

    GardenDb gardenDb();

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
}
