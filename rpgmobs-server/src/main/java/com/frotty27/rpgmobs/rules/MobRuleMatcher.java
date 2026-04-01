package com.frotty27.rpgmobs.rules;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.utils.StringHelpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobRuleMatcher {

    private static final int EXACT_BASE_SCORE = 3000;
    private static final int PREFIX_BASE_SCORE = 2000;
    private static final int CONTAINS_BASE_SCORE = 1000;

    public enum MatchKind {
        EXACT, PREFIX, CONTAINS
    }

    public record MatchResult(String key, RPGMobsConfig.MobRule mobRule, MatchKind matchKind, int score) {
    }

    // --- Match result cache (invalidated on config reload) ---

    private final Map<String, MatchResult> matchCache = new HashMap<>();
    private static final MatchResult NO_MATCH = new MatchResult("", null, MatchKind.EXACT, -1);

    public void clearCache() {
        matchCache.clear();
    }

    // --- Pre-normalization (call once on config load, not per tick) ---

    public static void preNormalizeMobRules(Map<String, RPGMobsConfig.MobRule> mobRules) {
        if (mobRules == null) return;
        for (RPGMobsConfig.MobRule rule : mobRules.values()) {
            if (rule == null) continue;
            rule.matchExact = normalizeList(rule.matchExact);
            rule.matchStartsWith = normalizeList(rule.matchStartsWith);
            rule.matchContains = normalizeList(rule.matchContains);
            rule.matchExcludes = normalizeList(rule.matchExcludes);
        }
    }

    private static List<String> normalizeList(List<String> list) {
        if (list == null || list.isEmpty()) return List.of();
        List<String> result = new ArrayList<>(list.size());
        for (String entry : list) {
            String normalized = StringHelpers.normalizeLower(entry);
            if (!normalized.isEmpty()) result.add(normalized);
        }
        return List.copyOf(result);
    }

    // --- Matching ---

    public MatchResult findBestMatch(Map<String, RPGMobsConfig.MobRule> mobRules, String roleName) {
        if (roleName == null || roleName.isBlank()) return null;
        if (mobRules == null || mobRules.isEmpty()) return null;

        final String lowerCaseRoleName = StringHelpers.normalizeLower(roleName);

        MatchResult cached = matchCache.get(lowerCaseRoleName);
        if (cached != null) {
            return cached == NO_MATCH ? null : cached;
        }

        MatchResult bestMatchResult = null;
        int bestScore = Integer.MIN_VALUE;

        for (Map.Entry<String, RPGMobsConfig.MobRule> mobRuleEntry : mobRules.entrySet()) {
            RPGMobsConfig.MobRule rule = mobRuleEntry.getValue();
            if (rule == null || !rule.enabled) continue;

            if (roleNameContainsAnyDeniedId(lowerCaseRoleName, rule.matchExcludes)) continue;

            ScoredMatch scoredMatch = scoreRule(lowerCaseRoleName, rule);
            if (scoredMatch == null) continue;

            if (scoredMatch.score > bestScore) {
                bestScore = scoredMatch.score;
                bestMatchResult = new MatchResult(mobRuleEntry.getKey(), rule, scoredMatch.kind, scoredMatch.score);
            } else if (scoredMatch.score == bestScore && bestMatchResult != null) {
                String mobRuleEntryKey = mobRuleEntry.getKey();
                if (mobRuleEntryKey != null && bestMatchResult.key() != null && mobRuleEntryKey.compareTo(
                        bestMatchResult.key()) < 0) {
                    bestMatchResult = new MatchResult(mobRuleEntry.getKey(), rule, scoredMatch.kind, scoredMatch.score);
                }
            }
        }

        matchCache.put(lowerCaseRoleName, bestMatchResult != null ? bestMatchResult : NO_MATCH);
        return bestMatchResult;
    }

    private record ScoredMatch(MatchKind kind, int score) {
    }

    private static ScoredMatch scoreRule(String roleLower, RPGMobsConfig.MobRule rule) {

        int exactMatchLength = longestExactMatchLength(roleLower, rule.matchExact);
        if (exactMatchLength > 0) return new ScoredMatch(MatchKind.EXACT, EXACT_BASE_SCORE + exactMatchLength);

        int prefixMatchLength = longestPrefixMatchLength(roleLower, rule.matchStartsWith);
        if (prefixMatchLength > 0) return new ScoredMatch(MatchKind.PREFIX, PREFIX_BASE_SCORE + prefixMatchLength);

        int containsMatchLength = longestContainsMatchLength(roleLower, rule.matchContains);
        if (containsMatchLength > 0) return new ScoredMatch(MatchKind.CONTAINS,
                                                            CONTAINS_BASE_SCORE + containsMatchLength
        );

        return null;
    }

    private static boolean roleNameContainsAnyDeniedId(String roleLower, List<String> matchExcludeList) {
        if (matchExcludeList == null || matchExcludeList.isEmpty()) return false;

        for (String excludeEntry : matchExcludeList) {
            if (roleLower.contains(excludeEntry)) return true;
        }
        return false;
    }

    // Inlined match methods -- no BiPredicate, direct String calls for JIT inlining

    private static int longestExactMatchLength(String roleLower, List<String> matchList) {
        if (matchList == null || matchList.isEmpty()) return 0;
        for (String entry : matchList) {
            if (roleLower.equals(entry)) return entry.length();
        }
        return 0;
    }

    private static int longestPrefixMatchLength(String roleLower, List<String> matchList) {
        if (matchList == null || matchList.isEmpty()) return 0;
        int bestLength = 0;
        for (String entry : matchList) {
            if (roleLower.startsWith(entry)) {
                bestLength = Math.max(bestLength, entry.length());
            }
        }
        return bestLength;
    }

    private static int longestContainsMatchLength(String roleLower, List<String> matchList) {
        if (matchList == null || matchList.isEmpty()) return 0;
        int bestLength = 0;
        for (String entry : matchList) {
            if (roleLower.contains(entry)) {
                bestLength = Math.max(bestLength, entry.length());
            }
        }
        return bestLength;
    }
}
