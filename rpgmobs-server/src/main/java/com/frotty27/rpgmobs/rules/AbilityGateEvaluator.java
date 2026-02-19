package com.frotty27.rpgmobs.rules;

import com.frotty27.rpgmobs.config.InstancesConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;
import static com.frotty27.rpgmobs.utils.StringHelpers.normalizeLower;
import static com.frotty27.rpgmobs.utils.StringHelpers.toLowerOrEmpty;

public final class AbilityGateEvaluator {

    private AbilityGateEvaluator() {
    }

    public static boolean isAllowed(RPGMobsConfig.AbilityConfig abilityConfig, String roleName, String weaponId,
                                    int tierIndex) {
        return isAllowed(abilityConfig, null, roleName, weaponId, tierIndex, null);
    }

    public static boolean isAllowed(RPGMobsConfig.AbilityConfig abilityConfig, @Nullable String abilityId,
                                    String roleName, String weaponId, int tierIndex,
                                    InstancesConfig.@Nullable InstanceRule instanceRule) {
        if (abilityConfig == null || !abilityConfig.isEnabled) return false;

        // Instance-level master toggle
        if (instanceRule != null && instanceRule.abilitiesEnabled != null && !instanceRule.abilitiesEnabled) return false;

        // Instance-level per-ability per-tier override takes highest priority over blanket
        boolean[] instanceTierOverride = resolveInstanceTierOverride(abilityId, instanceRule);
        if (instanceTierOverride != null) {
            if (!isEnabledForTier(instanceTierOverride, tierIndex)) return false;
        }

        // Global per-ability tier config
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

    /**
     * Resolves the instance-level per-tier override for a specific ability.
     * Priority: abilityOverrides[abilityId] > abilitiesEnabledPerTier > null (no override).
     */
    @SuppressWarnings("unchecked")
    private static boolean @Nullable [] resolveInstanceTierOverride(@Nullable String abilityId,
                                                                     InstancesConfig.@Nullable InstanceRule instanceRule) {
        if (instanceRule == null) return null;

        // Per-ability override takes priority
        if (abilityId != null && instanceRule.abilityOverrides != null) {
            Object raw = instanceRule.abilityOverrides.get(abilityId);
            if (raw instanceof boolean[] arr) {
                return arr;
            } else if (raw instanceof List<?> list) {
                // YAML deserializer may produce List<Boolean> instead of boolean[]
                boolean[] converted = new boolean[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Object elem = list.get(i);
                    converted[i] = elem instanceof Boolean b && b;
                }
                // Replace in map so conversion only happens once
                instanceRule.abilityOverrides.put(abilityId, converted);
                return converted;
            }
        }

        // Blanket per-tier fallback
        return instanceRule.abilitiesEnabledPerTier;
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
