package com.frotty27.rpgmobs.rules;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.utils.MobRuleCategoryHelpers;
import com.hypixel.hytale.logger.HytaleLogger;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;

public final class AbilityGateEvaluator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private AbilityGateEvaluator() {
    }

    public static boolean isAllowed(RPGMobsConfig.AbilityConfig abilityConfig, @Nullable String abilityId,
                                    String weaponId, int tierIndex, @Nullable String matchedRuleKey,
                                    @Nullable ResolvedConfig resolved) {
        if (abilityConfig == null || !abilityConfig.isEnabled) return false;

        int clamped = clampTierIndex(tierIndex);

        ResolvedConfig.ResolvedAbilityConfig resolvedAbility = null;
        if (resolved != null && abilityId != null) {
            resolvedAbility = resolved.resolvedAbilities.get(abilityId);
        }

        if (resolvedAbility != null) {
            if (!resolvedAbility.enabled) return false;
        } else {
            if (!isEnabledForTier(abilityConfig.isEnabledPerTier, tierIndex)) return false;
        }

        RPGMobsConfig.AbilityGate gate = abilityConfig.gate;
        if (gate != null && gate.allowedWeaponCategories != null && !gate.allowedWeaponCategories.isEmpty()) {
            RPGMobsConfig.GearCategory weaponTree = resolved != null ? resolved.weaponCategoryTree : null;
            if (weaponTree != null) {
                if (!weaponInCategories(weaponId, gate.allowedWeaponCategories, weaponTree)) return false;
            }
        }

        if (matchedRuleKey == null || matchedRuleKey.isBlank()) return false;

        if (abilityConfig.excludeLinkedMobRuleKeys != null
                && abilityConfig.excludeLinkedMobRuleKeys.contains(matchedRuleKey)) return false;

        if (resolvedAbility != null) {
            boolean[] entryTiers = resolvedAbility.linkedMobEntries.get(matchedRuleKey);
            if (entryTiers != null) {
                return clamped < 0 || clamped >= entryTiers.length || entryTiers[clamped];
            }
            for (Map.Entry<String, boolean[]> entry : resolvedAbility.linkedMobEntries.entrySet()) {
                if (MobRuleCategoryHelpers.isCategoryKey(entry.getKey())) {
                    String catName = MobRuleCategoryHelpers.fromCategoryKey(entry.getKey());
                    if (resolved != null && MobRuleCategoryHelpers.isMobKeyInCategory(
                            resolved.mobRuleCategoryTree, catName, matchedRuleKey)) {
                        boolean[] catTiers = entry.getValue();
                        return clamped < 0 || clamped >= catTiers.length || catTiers[clamped];
                    }
                }
            }
        } else {
            List<String> linkedKeys = abilityConfig.linkedMobRuleKeys;
            if (linkedKeys == null || linkedKeys.isEmpty()) return false;
            if (linkedKeys.contains(matchedRuleKey)) return true;
            RPGMobsConfig.MobRuleCategory tree = resolved != null ? resolved.mobRuleCategoryTree : null;
            if (tree != null) {
                for (String key : linkedKeys) {
                    if (MobRuleCategoryHelpers.isCategoryKey(key)) {
                        String catName = MobRuleCategoryHelpers.fromCategoryKey(key);
                        if (MobRuleCategoryHelpers.isMobKeyInCategory(tree, catName, matchedRuleKey)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isEnabledForTier(boolean[] enabledPerTier, int tierIndex) {
        if (enabledPerTier == null) return true;
        int clamped = clampTierIndex(tierIndex);
        if (clamped < 0 || clamped >= enabledPerTier.length) return false;
        return enabledPerTier[clamped];
    }

    private static boolean weaponInCategories(String weaponId, List<String> allowedCategories,
                                               RPGMobsConfig.GearCategory weaponTree) {
        if (allowedCategories == null || allowedCategories.isEmpty()) return true;
        if (weaponTree == null) return true;
        for (String catName : allowedCategories) {
            String plainName = MobRuleCategoryHelpers.isCategoryKey(catName)
                    ? MobRuleCategoryHelpers.fromCategoryKey(catName) : catName;
            RPGMobsConfig.GearCategory cat = MobRuleCategoryHelpers.findGearCategoryByName(weaponTree, plainName);
            if (cat != null && MobRuleCategoryHelpers.collectAllGearItemKeys(cat).contains(weaponId)) return true;
        }
        return false;
    }
}
