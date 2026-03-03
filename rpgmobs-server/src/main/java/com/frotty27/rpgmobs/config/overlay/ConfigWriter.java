package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.jspecify.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

public final class ConfigWriter {

    private static final Logger LOGGER = Logger.getLogger(ConfigWriter.class.getName());

    private ConfigWriter() {}

    public static void writeOverlay(ConfigOverlay overlay, Path filePath) throws IOException {
        Map<String, Object> yamlMap = overlayToMap(overlay);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndicatorIndent(2);
        options.setIndent(4);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        Yaml yaml = new Yaml(options);

        Files.createDirectories(filePath.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write("# RPGMobs overlay — only overridden fields are stored here.\n");
            writer.write("# Fields not present inherit from base config.\n\n");
            if (yamlMap.isEmpty()) {
                writer.write("# (no overrides)\n");
            } else {
                yaml.dump(yamlMap, writer);
            }
        }

        LOGGER.info(String.format("[RPGMobs] Wrote overlay: %s (%d fields)", filePath.getFileName(), yamlMap.size()));
    }

    static Map<String, Object> overlayToMap(ConfigOverlay overlay) {
        Map<String, Object> map = new LinkedHashMap<>();

        putIfNotNull(map, "enabled", overlay.enabled);

        putIfNotNull(map, "progressionStyle", overlay.progressionStyle);
        putDoubleArrayIfNotNull(map, "spawnChancePerTier", overlay.spawnChancePerTier);

        if (overlay.environmentTierRules != null) {
            if (overlay.environmentTierRules.isEmpty()) {
                map.put("environmentTierRules", new LinkedHashMap<>());
            } else {
                Map<String, List<Double>> envMap = new LinkedHashMap<>();
                for (Map.Entry<String, double[]> entry : overlay.environmentTierRules.entrySet()) {
                    List<Double> list = new ArrayList<>();
                    for (double v : entry.getValue()) list.add(v);
                    envMap.put(entry.getKey(), list);
                }
                map.put("environmentTierRules", envMap);
            }
        }

        putIfNotNull(map, "distancePerTier", overlay.distancePerTier);
        putIfNotNull(map, "distanceBonusInterval", overlay.distanceBonusInterval);
        putIfNotNull(map, "distanceHealthBonusPerInterval", overlay.distanceHealthBonusPerInterval);
        putIfNotNull(map, "distanceDamageBonusPerInterval", overlay.distanceDamageBonusPerInterval);
        putIfNotNull(map, "distanceHealthBonusCap", overlay.distanceHealthBonusCap);
        putIfNotNull(map, "distanceDamageBonusCap", overlay.distanceDamageBonusCap);

        putIfNotNull(map, "enableHealthScaling", overlay.enableHealthScaling);
        putFloatArrayIfNotNull(map, "healthMultiplierPerTier", overlay.healthMultiplierPerTier);
        putIfNotNull(map, "healthRandomVariance", overlay.healthRandomVariance != null ? (double) overlay.healthRandomVariance : null);
        putIfNotNull(map, "enableDamageScaling", overlay.enableDamageScaling);
        putFloatArrayIfNotNull(map, "damageMultiplierPerTier", overlay.damageMultiplierPerTier);
        putIfNotNull(map, "damageRandomVariance", overlay.damageRandomVariance != null ? (double) overlay.damageRandomVariance : null);

        if (overlay.abilityOverlays != null && !overlay.abilityOverlays.isEmpty()) {
            Map<String, Object> aoMap = new LinkedHashMap<>();
            for (Map.Entry<String, ConfigOverlay.AbilityOverlay> entry : overlay.abilityOverlays.entrySet()) {
                Map<String, Object> abilMap = new LinkedHashMap<>();
                ConfigOverlay.AbilityOverlay ao = entry.getValue();
                if (ao.enabled != null) abilMap.put("enabled", ao.enabled);
                if (ao.linkedEntries != null) {
                    List<Map<String, Object>> entriesList = new ArrayList<>();
                    for (ConfigOverlay.AbilityLinkedEntry ale : ao.linkedEntries) {
                        Map<String, Object> entryMap = new LinkedHashMap<>();
                        entryMap.put("key", ale.key);
                        List<Boolean> tierList = new ArrayList<>();
                        for (boolean b : ale.enabledPerTier) tierList.add(b);
                        entryMap.put("enabledPerTier", tierList);
                        entriesList.add(entryMap);
                    }
                    abilMap.put("linkedEntries", entriesList);
                }
                if (!abilMap.isEmpty()) aoMap.put(entry.getKey(), abilMap);
            }
            if (!aoMap.isEmpty()) map.put("abilityOverlays", aoMap);
        }

        putIntArrayIfNotNull(map, "vanillaDroplistExtraRollsPerTier", overlay.vanillaDroplistExtraRollsPerTier);
        putIfNotNull(map, "dropWeaponChance", overlay.dropWeaponChance);
        putIfNotNull(map, "dropArmorPieceChance", overlay.dropArmorPieceChance);
        putIfNotNull(map, "dropOffhandItemChance", overlay.dropOffhandItemChance);
        putIfNotNull(map, "droppedGearDurabilityMin", overlay.droppedGearDurabilityMin);
        putIfNotNull(map, "droppedGearDurabilityMax", overlay.droppedGearDurabilityMax);
        putIfNotNull(map, "defaultLootTemplate", overlay.defaultLootTemplate);

        putIfNotNull(map, "eliteFriendlyFireDisabled", overlay.eliteFriendlyFireDisabled);
        putIfNotNull(map, "eliteFallDamageDisabled", overlay.eliteFallDamageDisabled);
        putIfNotNull(map, "eliteNoAggroOnElite", overlay.eliteNoAggroOnElite);

        putIfNotNull(map, "enableNameplates", overlay.enableNameplates);
        putIfNotNull(map, "nameplateMode", overlay.nameplateMode);
        putBooleanArrayIfNotNull(map, "nameplateTierEnabled", overlay.nameplateTierEnabled);
        if (overlay.nameplatePrefixPerTier != null) {
            map.put("nameplatePrefixPerTier", Arrays.asList(overlay.nameplatePrefixPerTier));
        }
        if (overlay.tierPrefixesByFamily != null && !overlay.tierPrefixesByFamily.isEmpty()) {
            map.put("tierPrefixesByFamily", new LinkedHashMap<>(overlay.tierPrefixesByFamily));
        }

        putIfNotNull(map, "enableModelScaling", overlay.enableModelScaling);
        putFloatArrayIfNotNull(map, "modelScalePerTier", overlay.modelScalePerTier);
        putIfNotNull(map, "modelScaleVariance", overlay.modelScaleVariance != null ? (double) overlay.modelScaleVariance : null);

        putIfNotNull(map, "rpgLevelingEnabled", overlay.rpgLevelingEnabled);
        putFloatArrayIfNotNull(map, "xpMultiplierPerTier", overlay.xpMultiplierPerTier);
        putIfNotNull(map, "xpBonusPerAbility", overlay.xpBonusPerAbility);
        putIfNotNull(map, "minionXPMultiplier", overlay.minionXPMultiplier);

        if (overlay.tierOverrides != null && !overlay.tierOverrides.isEmpty()) {
            Map<String, Object> toMap = new LinkedHashMap<>();
            for (Map.Entry<String, ConfigOverlay.TierOverride> entry : overlay.tierOverrides.entrySet()) {
                toMap.put(entry.getKey(), tierOverrideToMap(entry.getValue()));
            }
            map.put("tierOverrides", toMap);
        }

        if (overlay.mobRules != null) {
            if (overlay.mobRules.isEmpty()) {
                map.put("mobRules", new LinkedHashMap<>());
            } else {
                Map<String, Object> rulesMap = new LinkedHashMap<>();
                for (Map.Entry<String, RPGMobsConfig.MobRule> entry : overlay.mobRules.entrySet()) {
                    rulesMap.put(entry.getKey(), mobRuleToMap(entry.getValue()));
                }
                map.put("mobRules", rulesMap);
            }
        }
        if (overlay.mobRuleCategoryTree != null) {
            map.put("mobRuleCategoryTree", mobRuleCategoryToMap(overlay.mobRuleCategoryTree));
        }

        if (overlay.lootTemplates != null) {
            if (overlay.lootTemplates.isEmpty()) {
                map.put("lootTemplates", new LinkedHashMap<>());
            } else {
                Map<String, Object> templatesMap = new LinkedHashMap<>();
                for (Map.Entry<String, RPGMobsConfig.LootTemplate> entry : overlay.lootTemplates.entrySet()) {
                    templatesMap.put(entry.getKey(), lootTemplateToMap(entry.getValue()));
                }
                map.put("lootTemplates", templatesMap);
            }
        }
        if (overlay.lootTemplateCategoryTree != null) {
            map.put("lootTemplateCategoryTree", lootTemplateCategoryToMap(overlay.lootTemplateCategoryTree));
        }

        if (overlay.customPreset != null) {
            Map<String, Object> presetMap = overlayToMap(overlay.customPreset);
            if (!presetMap.isEmpty()) {
                map.put("customPreset", presetMap);
            }
        }

        return map;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, @Nullable Object value) {
        if (value != null) map.put(key, value);
    }

    private static void putDoubleArrayIfNotNull(Map<String, Object> map, String key, double @Nullable [] arr) {
        if (arr == null) return;
        List<Double> list = new ArrayList<>();
        for (double v : arr) list.add(v);
        map.put(key, list);
    }

    private static void putFloatArrayIfNotNull(Map<String, Object> map, String key, float @Nullable [] arr) {
        if (arr == null) return;
        List<Double> list = new ArrayList<>();
        for (float v : arr) list.add((double) v);
        map.put(key, list);
    }

    private static void putIntArrayIfNotNull(Map<String, Object> map, String key, int @Nullable [] arr) {
        if (arr == null) return;
        List<Integer> list = new ArrayList<>();
        for (int v : arr) list.add(v);
        map.put(key, list);
    }

    private static void putBooleanArrayIfNotNull(Map<String, Object> map, String key, boolean @Nullable [] arr) {
        if (arr == null) return;
        List<Boolean> list = new ArrayList<>();
        for (boolean v : arr) list.add(v);
        map.put(key, list);
    }

    private static Map<String, Object> extraDropRuleToMap(RPGMobsConfig.ExtraDropRule rule) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("itemId", rule.itemId);
        m.put("chance", rule.chance);
        List<Boolean> tierList = new ArrayList<>();
        for (boolean b : rule.enabledPerTier) tierList.add(b);
        m.put("enabledPerTier", tierList);
        m.put("minQty", rule.minQty);
        m.put("maxQty", rule.maxQty);
        return m;
    }

    private static Map<String, Object> tierOverrideToMap(ConfigOverlay.TierOverride to) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Boolean> allowedList = new ArrayList<>();
        for (boolean b : to.allowedTiers) allowedList.add(b);
        m.put("allowedTiers", allowedList);
        return m;
    }

    private static Map<String, Object> mobRuleToMap(RPGMobsConfig.MobRule rule) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", rule.enabled);
        if (rule.matchExact != null && !rule.matchExact.isEmpty()) m.put("matchExact", new ArrayList<>(rule.matchExact));
        if (rule.matchStartsWith != null && !rule.matchStartsWith.isEmpty()) m.put("matchStartsWith", new ArrayList<>(rule.matchStartsWith));
        if (rule.matchContains != null && !rule.matchContains.isEmpty()) m.put("matchContains", new ArrayList<>(rule.matchContains));
        if (rule.matchExcludes != null && !rule.matchExcludes.isEmpty()) m.put("matchExcludes", new ArrayList<>(rule.matchExcludes));
        putBooleanArrayIfNotNull(m, "enableWeaponOverrideForTier", rule.enableWeaponOverrideForTier);
        m.put("weaponOverrideMode", rule.weaponOverrideMode.name());
        if (rule.allowedWeaponCategories != null && !rule.allowedWeaponCategories.isEmpty()) m.put("allowedWeaponCategories", new ArrayList<>(rule.allowedWeaponCategories));
        if (rule.allowedArmorCategories != null && !rule.allowedArmorCategories.isEmpty()) m.put("allowedArmorCategories", new ArrayList<>(rule.allowedArmorCategories));
        if (rule.allowedArmorSlots != null && !rule.allowedArmorSlots.isEmpty()) m.put("allowedArmorSlots", new ArrayList<>(rule.allowedArmorSlots));
        return m;
    }

    private static Map<String, Object> mobRuleCategoryToMap(RPGMobsConfig.MobRuleCategory cat) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", cat.name);
        if (cat.mobRuleKeys != null && !cat.mobRuleKeys.isEmpty()) {
            m.put("mobRuleKeys", new ArrayList<>(cat.mobRuleKeys));
        }
        if (cat.children != null && !cat.children.isEmpty()) {
            List<Map<String, Object>> childList = new ArrayList<>();
            for (RPGMobsConfig.MobRuleCategory child : cat.children) {
                childList.add(mobRuleCategoryToMap(child));
            }
            m.put("children", childList);
        }
        return m;
    }

    private static Map<String, Object> lootTemplateToMap(RPGMobsConfig.LootTemplate template) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", template.name);
        if (template.linkedMobRuleKeys != null && !template.linkedMobRuleKeys.isEmpty()) {
            m.put("linkedMobRuleKeys", new ArrayList<>(template.linkedMobRuleKeys));
        }
        if (template.drops != null && !template.drops.isEmpty()) {
            List<Map<String, Object>> dropsList = new ArrayList<>();
            for (RPGMobsConfig.ExtraDropRule drop : template.drops) {
                dropsList.add(extraDropRuleToMap(drop));
            }
            m.put("drops", dropsList);
        }
        return m;
    }

    private static Map<String, Object> lootTemplateCategoryToMap(RPGMobsConfig.LootTemplateCategory cat) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", cat.name);
        if (cat.templateKeys != null && !cat.templateKeys.isEmpty()) {
            m.put("templateKeys", new ArrayList<>(cat.templateKeys));
        }
        if (cat.children != null && !cat.children.isEmpty()) {
            List<Map<String, Object>> childList = new ArrayList<>();
            for (RPGMobsConfig.LootTemplateCategory child : cat.children) {
                childList.add(lootTemplateCategoryToMap(child));
            }
            m.put("children", childList);
        }
        return m;
    }
}
