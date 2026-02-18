package com.frotty27.rpgmobs.rules;

import com.frotty27.rpgmobs.config.RPGMobsConfig;

import java.util.List;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.rpgmobs.utils.StringHelpers.normalizeLower;
import static com.frotty27.rpgmobs.utils.StringHelpers.toLowerOrEmpty;

public final class AbilityGateEvaluator {

    private AbilityGateEvaluator() {
    }

    public static boolean isAllowed(RPGMobsConfig.AbilityConfig abilityConfig, String roleName, String weaponId,
                                    int tierIndex) {
        if (abilityConfig == null || !abilityConfig.isEnabled) return false;
        if (!isEnabledForTier(abilityConfig.isEnabledPerTier, tierIndex)) return false;

        RPGMobsConfig.AbilityGate abilityGate = abilityConfig.gate;
        if (abilityGate == null) return true;

        String roleNameLowercase = toLowerOrEmpty(roleName);
        String weaponIdLowercase = toLowerOrEmpty(weaponId);


        boolean baseAllowed = matchesGate(roleNameLowercase,
                                          weaponIdLowercase,
                                          abilityGate.roleMustContain,
                                          abilityGate.roleMustNotContain,
                                          abilityGate.weaponIdMustContain,
                                          abilityGate.weaponIdMustNotContain
        );

        List<RPGMobsConfig.AbilityRule> rules = abilityGate.rules;
        if (rules == null) return baseAllowed;

        for (RPGMobsConfig.AbilityRule rule : rules) {
            if (rule == null || !rule.enabled) continue;
            if (!isEnabledForTier(rule.enabledPerTier, tierIndex)) continue;

            boolean matchesRule = matchesGate(roleNameLowercase,
                                              weaponIdLowercase,
                                              rule.roleMustContain,
                                              rule.roleMustNotContain,
                                              rule.weaponIdMustContain,
                                              rule.weaponIdMustNotContain
            );
            if (!matchesRule) continue;

            return !rule.deny;
        }

        return baseAllowed;
    }

    private static boolean isEnabledForTier(boolean[] enabledPerTier, int tierIndex) {
        if (enabledPerTier == null) return true;

        int clampedTierIndex = clampTierIndex(tierIndex);
        if (clampedTierIndex < 0 || clampedTierIndex >= enabledPerTier.length) return false;

        return enabledPerTier[clampedTierIndex];
    }

    private static boolean matchesGate(String roleNameLowercase, String weaponIdLowercase, List<String> roleMustContain,
                                       List<String> roleMustNotContain, List<String> weaponIdMustContain,
                                       List<String> weaponIdMustNotContain) {
        if (!containsAnyRequired(roleNameLowercase, roleMustContain)) return false;
        if (containsAnyDenied(roleNameLowercase, roleMustNotContain)) return false;

        if (!containsAnyRequired(weaponIdLowercase, weaponIdMustContain)) return false;
        return !containsAnyDenied(weaponIdLowercase, weaponIdMustNotContain);
    }

    private static boolean containsAnyRequired(String haystackLowercase, List<String> requiredFragments) {
        if (requiredFragments == null || requiredFragments.isEmpty()) return true;

        for (String requiredFragment : requiredFragments) {
            if (requiredFragment == null || requiredFragment.isBlank()) continue;
            if (haystackLowercase.contains(normalizeLower(requiredFragment))) return true;
        }

        return false;
    }

    private static boolean containsAnyDenied(String haystackLowercase, List<String> deniedFragments) {
        if (deniedFragments == null || deniedFragments.isEmpty()) return false;

        for (String deniedFragment : deniedFragments) {
            if (deniedFragment == null || deniedFragment.isBlank()) continue;
            if (haystackLowercase.contains(normalizeLower(deniedFragment))) return true;
        }

        return false;
    }
}
