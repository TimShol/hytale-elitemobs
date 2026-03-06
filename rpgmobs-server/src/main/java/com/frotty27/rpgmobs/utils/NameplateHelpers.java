package com.frotty27.rpgmobs.utils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class NameplateHelpers {

    private static final String DEFAULT_NPC_NAME = "NPC";
    private static final String DEFAULT_FAMILY_KEY = "default";
    private static final Set<String> NOISE_SEGMENTS = Set.of("patrol", "wander", "big", "small");
    private static final Set<String> VARIANT_SEGMENTS = Set.of("burnt", "frost", "sand", "pirate",
                                                                "incandescent", "aberrant", "void");

    private NameplateHelpers() {}

    public static String prettifyString(String text) {
        if (text == null || text.isBlank()) return DEFAULT_NPC_NAME;

        String[] parts = text.replace('_', ' ').split("\\s+");
        StringBuilder pretty = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) continue;

            if (!pretty.isEmpty()) pretty.append(' ');
            pretty.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) pretty.append(part.substring(1).toLowerCase(Locale.ROOT));
        }

        return pretty.toString();
    }

    public static boolean isNoiseSegment(String segment) {
        if (segment == null) return true;
        return NOISE_SEGMENTS.contains(segment.toLowerCase(Locale.ROOT));
    }

    public static boolean isVariantSegment(String segment) {
        if (segment == null) return false;
        return VARIANT_SEGMENTS.contains(segment.toLowerCase(Locale.ROOT));
    }

    public static String classifyFamily(String roleName, Map<String, List<String>> tierPrefixesByFamily) {
        if (roleName == null || tierPrefixesByFamily == null || tierPrefixesByFamily.isEmpty()) {
            return DEFAULT_FAMILY_KEY;
        }
        String roleNameLower = roleName.toLowerCase(Locale.ROOT);
        String bestKey = DEFAULT_FAMILY_KEY;
        int bestLen = -1;
        for (String key : tierPrefixesByFamily.keySet()) {
            if (key == null || key.isBlank() || key.equals(DEFAULT_FAMILY_KEY)) continue;
            String keyLower = key.toLowerCase(Locale.ROOT);
            if (roleNameLower.contains(keyLower) && keyLower.length() > bestLen) {
                bestKey = key;
                bestLen = keyLower.length();
            }
        }
        return bestKey;
    }

    public static String resolveRoleWithoutFamily(String roleName) {
        if (roleName == null || roleName.isBlank()) return DEFAULT_NPC_NAME;

        String[] segments = roleName.split("_");
        if (segments.length <= 1) return prettifyString(roleName);

        if (segments.length == 2 && isVariantSegment(segments[1])) {
            return prettifyString(segments[0]);
        }

        return prettifyString(joinSegments(segments, 1, segments.length));
    }

    public static String resolveDisplayRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) return DEFAULT_NPC_NAME;

        String[] segments = roleName.split("_");
        if (segments.length == 0) return DEFAULT_NPC_NAME;

        int endExclusive = segments.length;
        while (endExclusive > 0 && isNoiseSegment(segments[endExclusive - 1])) {
            endExclusive--;
        }
        if (endExclusive <= 0) return DEFAULT_NPC_NAME;

        int startInclusive = 1;
        if (endExclusive > 1 && isVariantSegment(segments[1])) {
            startInclusive = 2;
        }

        if (endExclusive <= startInclusive) {
            if (segments.length >= 2 && startInclusive == 2 && isVariantSegment(segments[1])) {
                return prettifyString(segments[0]);
            }
            return prettifyString(joinSegments(segments, 0, endExclusive));
        }

        return prettifyString(joinSegments(segments, startInclusive, endExclusive));
    }

    static String joinSegments(String[] segments, int startInclusive, int endExclusive) {
        StringBuilder joined = new StringBuilder();

        for (int index = startInclusive; index < endExclusive; index++) {
            String segment = segments[index];
            if (segment == null || segment.isBlank()) continue;

            if (!joined.isEmpty()) joined.append('_');
            joined.append(segment);
        }

        return joined.toString();
    }
}
