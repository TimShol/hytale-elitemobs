package com.frotty27.rpgmobs.services;

import com.frotty27.rpgmobs.assets.CaeConfigGenerator;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.features.RPGMobsAbilityFeatureHelpers;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class RuntimeRoleSelector {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final int TIERS = 5;

    private final Map<String, Integer> variantIndexCache = new HashMap<>();
    private boolean initialized = false;

    public void initialize(RPGMobsConfig config) {
        variantIndexCache.clear();
        int found = 0;

        Set<String> allMobRuleKeys = collectAllMobRuleKeys(config);
        Set<String> allWeaponCategories = collectAllWeaponCategories(config);

        for (String mobRuleKey : allMobRuleKeys) {
            for (String weapon : allWeaponCategories) {
                for (int tier = 1; tier <= TIERS; tier++) {
                    String name = buildVariantName(mobRuleKey, weapon, tier);
                    int idx = NPCPlugin.get().getIndex(name);
                    if (idx >= 0) {
                        variantIndexCache.put(name, idx);
                        found++;
                    }
                }
            }
        }

        initialized = true;
        RPGMobsLogger.debug(LOGGER, "[RuntimeRoleSelector] Initialized: %d cached variants from %d mob rules x %d weapons",
                RPGMobsLogLevel.INFO, found, allMobRuleKeys.size(), allWeaponCategories.size());
    }

    public int resolveVariantRoleIndex(
            @Nonnull String matchedRuleKey,
            int tierIndex,
            @Nonnull Ref<EntityStore> npcRef,
            @Nonnull Store<EntityStore> entityStore,
            RPGMobsConfig.MobRule mobRule,
            @Nonnull RPGMobsPlugin plugin
    ) {
        if (!initialized) return -1;

        if (isNonHumanoid(matchedRuleKey)) {
            RPGMobsLogger.debug(LOGGER, "[RuntimeRoleSelector] Skipping non-humanoid: %s",
                    RPGMobsLogLevel.INFO, matchedRuleKey);
            return -1;
        }

        String weaponCategory = resolveWeaponCategory(npcRef, entityStore, plugin);
        if (weaponCategory == null) {
            RPGMobsLogger.debug(LOGGER, "[RuntimeRoleSelector] No weapon category resolved for %s",
                    RPGMobsLogLevel.INFO, matchedRuleKey);
            return -1;
        }

        RPGMobsConfig.CombatAIConfig combatAI = plugin.getConfig().combatAIConfig;
        if (combatAI != null && combatAI.weaponParams != null) {
            var weaponParams = combatAI.weaponParams.get(weaponCategory);
            if (weaponParams != null && weaponParams.isRanged) {
                RPGMobsLogger.debug(LOGGER, "[RuntimeRoleSelector] Skipping ranged weapon %s for %s - keeping vanilla ranged AI",
                        RPGMobsLogLevel.INFO, weaponCategory, matchedRuleKey);
                return -1;
            }
        }

        String variantName = buildVariantName(matchedRuleKey, weaponCategory, tierIndex + 1);
        int idx = variantIndexCache.getOrDefault(variantName, -1);

        RPGMobsLogger.debug(LOGGER,
                "[RuntimeRoleSelector] %s -> weapon=%s tier=T%d -> variant=%s -> idx=%d",
                RPGMobsLogLevel.INFO, matchedRuleKey, weaponCategory, tierIndex + 1, variantName, idx);

        return idx;
    }

    public static String buildVariantName(String mobRuleKey, String weapon, int tier) {
        return "RPGMobs_" + mobRuleKey + "_" + weapon + "_T" + tier;
    }

    private static Set<String> collectAllMobRuleKeys(RPGMobsConfig config) {
        Set<String> keys = new LinkedHashSet<>();
        if (config.mobsConfig != null && config.mobsConfig.defaultMobRules != null) {
            for (var entry : config.mobsConfig.defaultMobRules.entrySet()) {
                if (entry.getValue() != null && entry.getValue().enabled) {
                    keys.add(entry.getKey());
                }
            }
        }
        return keys;
    }

    private static Set<String> collectAllWeaponCategories(RPGMobsConfig config) {
        Set<String> categories = new LinkedHashSet<>();
        if (config.combatAIConfig.weaponParams != null) {
            categories.addAll(config.combatAIConfig.weaponParams.keySet());
        }
        return categories;
    }

    private static String resolveWeaponCategory(
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            RPGMobsPlugin plugin
    ) {
        String weaponId = RPGMobsAbilityFeatureHelpers.resolveWeaponId(npcRef, entityStore);
        if (weaponId.isEmpty()) return null;

        RPGMobsConfig config = plugin.getConfig();
        if (config == null || config.gearConfig.weaponCategoryTree == null) {
            return null;
        }

        RPGMobsConfig.GearCategory weaponTree = config.gearConfig.weaponCategoryTree;
        for (RPGMobsConfig.GearCategory category : weaponTree.children) {
            if (category.itemKeys.contains(weaponId)) {
                if ("Clubs".equals(category.name) && weaponId.toLowerCase().contains("flail")) {
                    return "ClubsFlail";
                }
                return category.name;
            }
        }

        return null;
    }

    private static boolean isNonHumanoid(String roleName) {
        return CaeConfigGenerator.isNonHumanoid(roleName);
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getCachedVariantCount() {
        return variantIndexCache.size();
    }
}
