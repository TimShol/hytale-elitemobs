package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigPipelineRoundTripTest {

    @Test
    public void overlayWithScalarBooleanRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        overlay.enabled = false;
        overlay.enableHealthScaling = true;
        overlay.enableDamageScaling = false;
        overlay.enableNameplates = false;

        var restored = writeAndReload(overlay, tempDir);

        assertEquals(false, restored.enabled);
        assertEquals(true, restored.enableHealthScaling);
        assertEquals(false, restored.enableDamageScaling);
        assertEquals(false, restored.enableNameplates);
    }

    @Test
    public void overlayWithScalarFloatAndDoubleRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        overlay.distancePerTier = 2000.0;
        overlay.distanceBonusInterval = 200.0;
        overlay.distanceHealthBonusPerInterval = 0.05f;
        overlay.distanceDamageBonusPerInterval = 0.02f;
        overlay.dropWeaponChance = 0.15;
        overlay.dropArmorPieceChance = 0.25;
        overlay.healthRandomVariance = 0.1f;
        overlay.xpBonusPerAbility = 5000.0;
        overlay.minionXPMultiplier = 0.1;

        var restored = writeAndReload(overlay, tempDir);

        assertEquals(2000.0, restored.distancePerTier);
        assertEquals(200.0, restored.distanceBonusInterval);
        assertEquals(0.05f, restored.distanceHealthBonusPerInterval, 0.001f);
        assertEquals(0.02f, restored.distanceDamageBonusPerInterval, 0.001f);
        assertEquals(0.15, restored.dropWeaponChance);
        assertEquals(0.25, restored.dropArmorPieceChance);
        assertEquals(0.1f, restored.healthRandomVariance, 0.001f);
        assertEquals(5000.0, restored.xpBonusPerAbility);
        assertEquals(0.1, restored.minionXPMultiplier);
    }

    @Test
    public void overlayWithFloatArrayFieldsRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        overlay.healthMultiplierPerTier = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
        overlay.damageMultiplierPerTier = new float[]{0.5f, 1.0f, 1.5f, 2.0f, 2.5f};
        overlay.modelScalePerTier = new float[]{0.8f, 0.9f, 1.0f, 1.1f, 1.2f};
        overlay.xpMultiplierPerTier = new float[]{1.0f, 1.5f, 2.0f, 3.0f, 5.0f};

        var restored = writeAndReload(overlay, tempDir);

        assertArrayEquals(overlay.healthMultiplierPerTier, restored.healthMultiplierPerTier, 0.001f);
        assertArrayEquals(overlay.damageMultiplierPerTier, restored.damageMultiplierPerTier, 0.001f);
        assertArrayEquals(overlay.modelScalePerTier, restored.modelScalePerTier, 0.001f);
        assertArrayEquals(overlay.xpMultiplierPerTier, restored.xpMultiplierPerTier, 0.001f);
    }

    @Test
    public void overlayWithIntAndBooleanArraysRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        overlay.vanillaDroplistExtraRollsPerTier = new int[]{0, 1, 2, 3, 4};
        overlay.nameplateTierEnabled = new boolean[]{true, false, true, false, true};

        var restored = writeAndReload(overlay, tempDir);

        assertArrayEquals(new int[]{0, 1, 2, 3, 4}, restored.vanillaDroplistExtraRollsPerTier);
        assertArrayEquals(new boolean[]{true, false, true, false, true}, restored.nameplateTierEnabled);
    }

    @Test
    public void overlayWithMobRulesRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();

        overlay.disabledMobRuleKeys = new LinkedHashSet<>(List.of("zombie_scout", "skeleton_warrior"));

        var restored = writeAndReload(overlay, tempDir);

        assertNotNull(restored.disabledMobRuleKeys);
        assertEquals(2, restored.disabledMobRuleKeys.size());
        assertTrue(restored.disabledMobRuleKeys.contains("zombie_scout"));
        assertTrue(restored.disabledMobRuleKeys.contains("skeleton_warrior"));
    }

    @Test
    public void overlayWithLootTemplatesRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();

        var drop = new RPGMobsConfig.ExtraDropRule();
        drop.itemId = "Gold_Coin";
        drop.chance = 0.75;
        drop.enabledPerTier = new boolean[]{true, true, false, false, false};
        drop.minQty = 1;
        drop.maxQty = 5;

        var template = new RPGMobsConfig.LootTemplate();
        template.name = "GoblinLoot";
        template.linkedMobRuleKeys = new ArrayList<>(List.of("goblin_scout", "category:Goblins"));
        template.drops = new ArrayList<>(List.of(drop));

        overlay.lootTemplates = new LinkedHashMap<>();
        overlay.lootTemplates.put("GoblinLoot", template);

        var restored = writeAndReload(overlay, tempDir);

        assertNotNull(restored.lootTemplates);
        assertEquals(1, restored.lootTemplates.size());
        var restoredTpl = restored.lootTemplates.get("GoblinLoot");
        assertNotNull(restoredTpl);
        assertEquals("GoblinLoot", restoredTpl.name);
        assertEquals(List.of("goblin_scout", "category:Goblins"), restoredTpl.linkedMobRuleKeys);
        assertEquals(1, restoredTpl.drops.size());

        var restoredDrop = restoredTpl.drops.getFirst();
        assertEquals("Gold_Coin", restoredDrop.itemId);
        assertEquals(0.75, restoredDrop.chance, 0.001);
        assertArrayEquals(new boolean[]{true, true, false, false, false}, restoredDrop.enabledPerTier);
        assertEquals(1, restoredDrop.minQty);
        assertEquals(5, restoredDrop.maxQty);
    }

    @Test
    public void overlayWithAbilityOverlaysRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();

        var entry1 = new ConfigOverlay.AbilityLinkedEntry("zombie", new boolean[]{true, true, false, false, false});
        var entry2 = new ConfigOverlay.AbilityLinkedEntry("skeleton", new boolean[]{false, false, true, true, true});

        var abilOverlay = new ConfigOverlay.AbilityOverlay();
        abilOverlay.enabled = true;
        abilOverlay.linkedEntries = new ArrayList<>(List.of(entry1, entry2));

        overlay.abilityOverlays = new LinkedHashMap<>();
        overlay.abilityOverlays.put("charge_leap", abilOverlay);

        var restored = writeAndReload(overlay, tempDir);

        assertNotNull(restored.abilityOverlays);
        assertEquals(1, restored.abilityOverlays.size());
        var restoredAbil = restored.abilityOverlays.get("charge_leap");
        assertNotNull(restoredAbil);
        assertEquals(true, restoredAbil.enabled);
        assertNotNull(restoredAbil.linkedEntries);
        assertEquals(2, restoredAbil.linkedEntries.size());
        assertEquals("zombie", restoredAbil.linkedEntries.get(0).key);
        assertArrayEquals(new boolean[]{true, true, false, false, false}, restoredAbil.linkedEntries.get(0).enabledPerTier);
        assertEquals("skeleton", restoredAbil.linkedEntries.get(1).key);
        assertArrayEquals(new boolean[]{false, false, true, true, true}, restoredAbil.linkedEntries.get(1).enabledPerTier);
    }

    @Test
    public void effectivelyEqualsTrueAfterRoundTrip(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        overlay.enabled = false;
        overlay.distancePerTier = 2000.0;
        overlay.healthMultiplierPerTier = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
        overlay.vanillaDroplistExtraRollsPerTier = new int[]{1, 2, 3, 4, 5};
        overlay.nameplateTierEnabled = new boolean[]{true, false, true, false, true};
        overlay.enableModelScaling = false;
        overlay.dropWeaponChance = 0.33;

        var restored = writeAndReload(overlay, tempDir);
        var base = new ResolvedConfig();

        assertTrue(ConfigOverlay.effectivelyEquals(overlay, restored, base));
    }

    @Test
    public void mergedConfigOverridesBase() {
        var base = new ResolvedConfig();
        base.healthMultiplierPerTier = new float[]{0.3f, 0.6f, 1.2f, 1.8f, 2.6f};
        base.enabled = true;
        base.dropWeaponChance = 0.05;

        var overlay = new ConfigOverlay();
        overlay.healthMultiplierPerTier = new float[]{10f, 20f, 30f, 40f, 50f};
        overlay.enabled = false;
        overlay.dropWeaponChance = 0.99;

        var merged = new ResolvedConfig();
        OverlayFieldRegistry.mergeAll(overlay, base, merged);

        assertFalse(merged.enabled);
        assertArrayEquals(new float[]{10f, 20f, 30f, 40f, 50f}, merged.healthMultiplierPerTier);
        assertEquals(0.99, merged.dropWeaponChance, 0.001);
    }

    @Test
    public void mergedConfigInheritsBaseForNullOverlay() {
        var base = new ResolvedConfig();
        base.enabled = true;
        base.enableHealthScaling = true;
        base.distancePerTier = 1000.0;
        base.healthMultiplierPerTier = new float[]{1f, 2f, 3f, 4f, 5f};
        base.dropWeaponChance = 0.05;

        var overlay = new ConfigOverlay();

        var merged = new ResolvedConfig();
        OverlayFieldRegistry.mergeAll(overlay, base, merged);

        assertTrue(merged.enabled);
        assertTrue(merged.enableHealthScaling);
        assertEquals(1000.0, merged.distancePerTier);
        assertArrayEquals(new float[]{1f, 2f, 3f, 4f, 5f}, merged.healthMultiplierPerTier);
        assertEquals(0.05, merged.dropWeaponChance, 0.001);
    }

    @Test
    public void mergedFloatArraysAreIndependentCopies() {
        var base = new ResolvedConfig();
        base.healthMultiplierPerTier = new float[]{1f, 2f, 3f, 4f, 5f};

        var overlay = new ConfigOverlay();
        overlay.healthMultiplierPerTier = new float[]{10f, 20f, 30f, 40f, 50f};

        var merged = new ResolvedConfig();
        OverlayFieldRegistry.mergeAll(overlay, base, merged);

        overlay.healthMultiplierPerTier[0] = 999f;
        assertEquals(10f, merged.healthMultiplierPerTier[0]);
    }

    @Test
    public void allNullOverlayIsEffectivelyEqualToItself() {
        var overlay1 = new ConfigOverlay();
        var overlay2 = new ConfigOverlay();
        var base = new ResolvedConfig();

        assertTrue(ConfigOverlay.effectivelyEquals(overlay1, overlay2, base));
    }

    @Test
    public void nullOverlayVsExplicitBaseValueIsEffectivelyEqual() {
        var overlay1 = new ConfigOverlay();
        var overlay2 = new ConfigOverlay();
        overlay2.enabled = true;

        var base = new ResolvedConfig();
        base.enabled = true;

        assertTrue(ConfigOverlay.effectivelyEquals(overlay1, overlay2, base));
    }

    @Test
    public void overlayWithTierOverridesRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        var tierOverride = new ConfigOverlay.TierOverride();
        tierOverride.allowedTiers = new boolean[]{true, true, false, false, false};
        overlay.tierOverrides = new LinkedHashMap<>();
        overlay.tierOverrides.put("zombie", tierOverride);

        var restored = writeAndReload(overlay, tempDir);

        assertNotNull(restored.tierOverrides);
        assertEquals(1, restored.tierOverrides.size());
        var restoredOverride = restored.tierOverrides.get("zombie");
        assertNotNull(restoredOverride);
        assertArrayEquals(new boolean[]{true, true, false, false, false}, restoredOverride.allowedTiers);
    }

    @Test
    public void overlayWithProgressionStyleRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        overlay.progressionStyle = "DISTANCE_FROM_SPAWN";

        var restored = writeAndReload(overlay, tempDir);

        assertEquals("DISTANCE_FROM_SPAWN", restored.progressionStyle);
    }

    @Test
    public void overlayWithSpawnChancePerTierRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        overlay.spawnChancePerTier = new double[]{0.1, 0.2, 0.3, 0.25, 0.15};

        var restored = writeAndReload(overlay, tempDir);

        assertNotNull(restored.spawnChancePerTier);
        assertArrayEquals(new double[]{0.1, 0.2, 0.3, 0.25, 0.15}, restored.spawnChancePerTier, 0.001);
    }

    @Test
    public void overlayWithEnvironmentTierRulesRoundTrips(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();
        overlay.environmentTierRules = new LinkedHashMap<>();
        overlay.environmentTierRules.put("zone0", new double[]{100, 0, 0, 0, 0});
        overlay.environmentTierRules.put("zone1", new double[]{50, 30, 20, 0, 0});

        var restored = writeAndReload(overlay, tempDir);

        assertNotNull(restored.environmentTierRules);
        assertEquals(2, restored.environmentTierRules.size());
        assertArrayEquals(new double[]{100, 0, 0, 0, 0}, restored.environmentTierRules.get("zone0"), 0.001);
        assertArrayEquals(new double[]{50, 30, 20, 0, 0}, restored.environmentTierRules.get("zone1"), 0.001);
    }

    @Test
    public void emptyOverlayRoundTripsAsAllNull(@TempDir Path tempDir) throws IOException {
        var overlay = new ConfigOverlay();

        var restored = writeAndReload(overlay, tempDir);

        assertNull(restored.enabled);
        assertNull(restored.enableHealthScaling);
        assertNull(restored.distancePerTier);
        assertNull(restored.healthMultiplierPerTier);
        assertNull(restored.vanillaDroplistExtraRollsPerTier);
        assertNull(restored.progressionStyle);
        assertNull(restored.spawnChancePerTier);
        assertNull(restored.environmentTierRules);
        assertNull(restored.disabledMobRuleKeys);
        assertNull(restored.lootTemplates);
        assertNull(restored.abilityOverlays);
        assertNull(restored.tierOverrides);
    }

    private ConfigOverlay writeAndReload(ConfigOverlay overlay, Path tempDir) throws IOException {
        Path file = tempDir.resolve("test-overlay.yml");
        ConfigWriter.writeOverlay(overlay, file);

        var yaml = new org.yaml.snakeyaml.Yaml();
        Map<String, Object> yamlMap;
        try (var reader = java.nio.file.Files.newBufferedReader(file)) {
            Object loaded = yaml.load(reader);
            if (loaded instanceof Map<?, ?> m) {
                yamlMap = new LinkedHashMap<>();
                for (var entry : m.entrySet()) {
                    if (entry.getKey() != null) {
                        yamlMap.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            } else {
                yamlMap = new LinkedHashMap<>();
            }
        }

        var restored = new ConfigOverlay();
        OverlayFieldRegistry.applyAllYaml(yamlMap, restored);

        if (yamlMap.containsKey("progressionStyle")) {
            restored.progressionStyle = String.valueOf(yamlMap.get("progressionStyle"));
        }
        if (yamlMap.containsKey("spawnChancePerTier")) {
            Object v = yamlMap.get("spawnChancePerTier");
            if (v instanceof List<?> list) {
                double[] arr = new double[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Object el = list.get(i);
                    arr[i] = (el instanceof Number n) ? n.doubleValue() : 0.0;
                }
                restored.spawnChancePerTier = arr;
            }
        }
        if (yamlMap.containsKey("environmentTierRules") && yamlMap.get("environmentTierRules") instanceof Map<?, ?> envMap) {
            Map<String, double[]> envRules = new LinkedHashMap<>();
            for (var entry : envMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof List<?> list) {
                    double[] arr = new double[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        Object el = list.get(i);
                        arr[i] = (el instanceof Number n) ? n.doubleValue() : 0.0;
                    }
                    envRules.put(key, arr);
                }
            }
            restored.environmentTierRules = envRules;
        }
        if (yamlMap.containsKey("disabledMobRuleKeys") && yamlMap.get("disabledMobRuleKeys") instanceof List<?> disabledList) {
            Set<String> disabledKeys = new LinkedHashSet<>();
            for (Object item : disabledList) {
                if (item != null) disabledKeys.add(String.valueOf(item));
            }
            restored.disabledMobRuleKeys = disabledKeys;
        }
        if (yamlMap.containsKey("lootTemplates") && yamlMap.get("lootTemplates") instanceof Map<?, ?> tplsMap) {
            Map<String, RPGMobsConfig.LootTemplate> templates = new LinkedHashMap<>();
            for (var entry : tplsMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> tplMap) {
                    Map<String, Object> tplMapStr = new LinkedHashMap<>();
                    for (var te : tplMap.entrySet()) {
                        if (te.getKey() != null) tplMapStr.put(String.valueOf(te.getKey()), te.getValue());
                    }
                    var tpl = new RPGMobsConfig.LootTemplate();
                    tpl.name = key;
                    tpl.linkedMobRuleKeys = ConfigResolver.getStringList(tplMapStr, "linkedMobRuleKeys");
                    Object dropsRaw = tplMapStr.get("drops");
                    if (dropsRaw instanceof List<?> dropList) {
                        tpl.drops = new ArrayList<>();
                        for (Object drop : dropList) {
                            if (drop instanceof Map<?, ?> dropMap) {
                                Map<String, Object> dropMapStr = new LinkedHashMap<>();
                                for (var de : dropMap.entrySet()) {
                                    if (de.getKey() != null) dropMapStr.put(String.valueOf(de.getKey()), de.getValue());
                                }
                                var rule = ConfigResolver.parseExtraDropRule(dropMapStr);
                                if (rule != null) tpl.drops.add(rule);
                            }
                        }
                    }
                    templates.put(key, tpl);
                }
            }
            restored.lootTemplates = templates;
        }
        if (yamlMap.containsKey("abilityOverlays") && yamlMap.get("abilityOverlays") instanceof Map<?, ?> abilOverlayMap) {
            Map<String, ConfigOverlay.AbilityOverlay> overlays = new LinkedHashMap<>();
            for (var entry : abilOverlayMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> abilMap) {
                    var ao = new ConfigOverlay.AbilityOverlay();
                    Object enabledObj = abilMap.get("enabled");
                    if (enabledObj instanceof Boolean b) ao.enabled = b;
                    Object linkedEntriesRaw = abilMap.get("linkedEntries");
                    if (linkedEntriesRaw instanceof List<?> entriesList) {
                        ao.linkedEntries = new ArrayList<>();
                        for (Object entryObj : entriesList) {
                            if (entryObj instanceof Map<?, ?> entryMap) {
                                var ale = new ConfigOverlay.AbilityLinkedEntry();
                                Object keyObj = entryMap.get("key");
                                ale.key = keyObj != null ? String.valueOf(keyObj) : "";
                                Object eptObj = entryMap.get("enabledPerTier");
                                if (eptObj instanceof List<?> eptList) {
                                    for (int i = 0; i < Math.min(eptList.size(), 5); i++) {
                                        Object val = eptList.get(i);
                                        if (val instanceof Boolean b) ale.enabledPerTier[i] = b;
                                    }
                                }
                                ao.linkedEntries.add(ale);
                            }
                        }
                    }
                    overlays.put(key, ao);
                }
            }
            if (!overlays.isEmpty()) restored.abilityOverlays = overlays;
        }
        if (yamlMap.containsKey("tierOverrides") && yamlMap.get("tierOverrides") instanceof Map<?, ?> tierMap) {
            Map<String, ConfigOverlay.TierOverride> overrides = new LinkedHashMap<>();
            for (var entry : tierMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> overrideMap) {
                    var to = new ConfigOverlay.TierOverride();
                    Object allowedRaw = overrideMap.get("allowedTiers");
                    if (allowedRaw instanceof List<?> list) {
                        for (int i = 0; i < Math.min(list.size(), 5); i++) {
                            Object val = list.get(i);
                            if (val instanceof Boolean b) to.allowedTiers[i] = b;
                        }
                    }
                    overrides.put(key, to);
                }
            }
            if (!overrides.isEmpty()) restored.tierOverrides = overrides;
        }

        return restored;
    }
}
