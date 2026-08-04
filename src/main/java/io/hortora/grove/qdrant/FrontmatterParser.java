package io.hortora.grove.qdrant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FrontmatterParser {

    public static Map<String, Object> parse(String rawContent) {
        if (rawContent == null || !rawContent.startsWith("---")) {
            return Collections.emptyMap();
        }

        int endIndex = rawContent.indexOf("\n---", 3);
        if (endIndex < 0) {
            return Collections.emptyMap();
        }

        String yamlBlock = rawContent.substring(4, endIndex).trim();
        Map<String, Object> result = new LinkedHashMap<>();

        for (String line : yamlBlock.split("\n")) {
            int colonIndex = line.indexOf(':');
            if (colonIndex < 0) continue;

            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();

            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            if (value.startsWith("[") && value.endsWith("]")) {
                String inner = value.substring(1, value.length() - 1).trim();
                if (inner.isEmpty()) {
                    result.put(key, Collections.emptyList());
                } else {
                    List<String> items = List.of(inner.split(",")).stream()
                            .map(String::trim)
                            .toList();
                    result.put(key, items);
                }
            } else if (value.equals("true")) {
                result.put(key, true);
            } else if (value.equals("false")) {
                result.put(key, false);
            } else {
                try {
                    result.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    public static String bodyWithoutFrontmatter(String rawContent) {
        if (rawContent == null || !rawContent.startsWith("---")) {
            return rawContent;
        }
        int endIndex = rawContent.indexOf("\n---", 3);
        if (endIndex < 0) {
            return rawContent;
        }
        return rawContent.substring(endIndex + 4).trim();
    }
}
