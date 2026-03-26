package com.frotty27.rpgmobs.rules;

import com.frotty27.rpgmobs.config.RPGMobsConfig;

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import static com.frotty27.rpgmobs.utils.StringHelpers.normalizeLower;

public final class MobRuleMatcher {

    private static final int EXACT_BASE_SCORE = 3000;
    private static final int PREFIX_BASE_SCORE = 2000;
    private static final int CONTAINS_BASE_SCORE = 1000;

    public enum MatchKind {
        EXACT, PREFIX, CONTAINS
    }

    public record MatchResult(String key, RPGMobsConfig.MobRule mobRule, MatchKind matchKind, int score) {
    }

    public MatchResult findBestMatch(Map<String, RPGMobsConfig.MobRule> mobRules, String roleName) {
        if (roleName == null || roleName.isBlank()) return null;

        if (mobRules == null || mobRules.isEmpty())
            return null;

        final String lowerCaseRoleName = normalizeLower(roleName);

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

    private static boolean roleNameContainsAnyDeniedId(String id, List<String> matchExcludeList) {
        if (matchExcludeList == null || matchExcludeList.isEmpty()) return false;

        for (String matchExcludeEntry : matchExcludeList) {
            String normalizedExcludeEntry = normalizeEntry(matchExcludeEntry);
            if (normalizedExcludeEntry.isEmpty()) continue;
            if (id.contains(normalizedExcludeEntry)) return true;
        }
        return false;
    }

    private static int longestExactMatchLength(String roleLower, List<String> matchList) {
        return longestMatchLength(roleLower, matchList, String::equals);
    }

    private static int longestPrefixMatchLength(String roleLower, List<String> matchList) {
        return longestMatchLength(roleLower, matchList, String::startsWith);
    }

    private static int longestContainsMatchLength(String roleLower, List<String> matchList) {
        return longestMatchLength(roleLower, matchList, String::contains);
    }

    private static int longestMatchLength(String roleLower, List<String> matchList,
                                          BiPredicate<String, String> matcher) {
        if (matchList == null || matchList.isEmpty()) return 0;

        int bestScore = 0;
        for (String entry : matchList) {
            var normalized = normalizeEntry(entry);
            if (normalized.isEmpty()) continue;
            if (matcher.test(roleLower, normalized)) {
                bestScore = Math.max(bestScore, normalized.length());
            }
        }
        return bestScore;
    }

    private static String normalizeEntry(String entry) {
        return normalizeLower(entry);
    }
}
