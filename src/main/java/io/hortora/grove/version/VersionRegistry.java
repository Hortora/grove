package io.hortora.grove.version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import io.hortora.grove.config.GroveConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class VersionRegistry {

    private final Path registryPath;
    private Map<String, String> versions;

    @Inject
    public VersionRegistry(GroveConfig config) {
        this(Path.of(config.garden().path()).resolve("version-registry.yml"));
    }

    public VersionRegistry(Path registryPath) {
        this.registryPath = registryPath;
    }

    public Map<String, String> getVersions() {
        if (versions == null) {
            versions = loadVersions();
        }
        return versions;
    }

    public void setVersion(String stack, String version) throws IOException {
        var current = new LinkedHashMap<>(getVersions());
        current.put(stack, version);
        saveVersions(current);
        this.versions = current;
    }

    public String getCurrentVersion(String stack) {
        return getVersions().get(stack.toLowerCase());
    }

    private Map<String, String> loadVersions() {
        var result = new LinkedHashMap<String, String>();
        if (!Files.exists(registryPath)) return result;
        try {
            for (String line : Files.readAllLines(registryPath)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int colonIdx = line.indexOf(':');
                if (colonIdx > 0) {
                    String key = line.substring(0, colonIdx).trim().toLowerCase();
                    String val = line.substring(colonIdx + 1).trim();
                    result.put(key, val);
                }
            }
        } catch (IOException e) {
            // return empty map
        }
        return result;
    }

    private void saveVersions(Map<String, String> versions) throws IOException {
        var lines = new java.util.ArrayList<String>();
        versions.forEach((k, v) -> lines.add(k + ": " + v));
        Files.createDirectories(registryPath.getParent());
        Files.writeString(registryPath, String.join("\n", lines) + "\n");
    }

    public void reload() {
        this.versions = null;
    }
}