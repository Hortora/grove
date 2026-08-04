package io.hortora.grove.version;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionScorer {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    public enum Tier { CURRENT, AGING, LEGACY, UNKNOWN }

    public static Tier score(String verifiedOn, Map<String, String> registry) {
        if (verifiedOn == null || verifiedOn.isBlank() || registry.isEmpty()) {
            return Tier.UNKNOWN;
        }

        String normalized = verifiedOn.trim().toLowerCase();

        for (Map.Entry<String, String> entry : registry.entrySet()) {
            String stack = entry.getKey().toLowerCase();
            if (normalized.startsWith(stack)) {
                String versionPart = normalized.substring(stack.length()).trim();
                return compareVersions(versionPart, entry.getValue());
            }
        }

        for (Map.Entry<String, String> entry : registry.entrySet()) {
            Matcher entryMatcher = VERSION_PATTERN.matcher(normalized);
            if (entryMatcher.find()) {
                return compareVersions(entryMatcher.group(), entry.getValue());
            }
        }

        return Tier.UNKNOWN;
    }

    static Tier compareVersions(String verified, String current) {
        int[] v = parseVersion(verified);
        int[] c = parseVersion(current);
        if (v == null || c == null) return Tier.UNKNOWN;

        if (v[0] < c[0]) return Tier.LEGACY;

        int minorDiff = c[1] - v[1];
        if (v[0] == c[0] && minorDiff >= 2) return Tier.AGING;
        if (v[0] == c[0] && minorDiff < 2) return Tier.CURRENT;

        return Tier.CURRENT;
    }

    private static int[] parseVersion(String version) {
        Matcher m = VERSION_PATTERN.matcher(version);
        if (!m.find()) return null;
        int major = Integer.parseInt(m.group(1));
        int minor = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
        int patch = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        return new int[]{major, minor, patch};
    }
}