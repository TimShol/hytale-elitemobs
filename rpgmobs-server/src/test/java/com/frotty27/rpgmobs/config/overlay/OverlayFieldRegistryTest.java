package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OverlayFieldRegistryTest {

    @Test
    void buildAllBasePopulatesResolvedFromConfig() {
        var config = new RPGMobsConfig();
        var resolved = new ResolvedConfig();

        OverlayFieldRegistry.buildAllBase(config, resolved);

        assertTrue(resolved.enabled);
        assertEquals(config.healthConfig.mobHealthRandomVariance, resolved.healthRandomVariance);
        assertEquals(config.spawning.distancePerTier, resolved.distancePerTier);
        assertEquals(config.damageConfig.enableMobDamageMultiplier, resolved.enableDamageScaling);
        assertArrayEquals(config.healthConfig.mobHealthMultiplierPerTier, resolved.healthMultiplierPerTier);
        assertArrayEquals(config.lootConfig.vanillaDroplistExtraRollsPerTier, resolved.vanillaDroplistExtraRollsPerTier);
        assertArrayEquals(config.nameplatesConfig.mobNameplatesEnabledPerTier, resolved.nameplateTierEnabled);
        assertArrayEquals(config.nameplatesConfig.monNameplatePrefixPerTier, resolved.nameplatePrefixPerTier);
        assertEquals(config.nameplatesConfig.nameplateMode.name(), resolved.nameplateMode);
        assertEquals(config.lootConfig.dropWeaponChance, resolved.dropWeaponChance);
        assertEquals(config.integrationsConfig.rpgLeveling.xpBonusPerAbility, resolved.xpBonusPerAbility);
        assertEquals(config.integrationsConfig.rpgLeveling.minionXPMultiplier, resolved.minionXPMultiplier);
    }

    @Test
    void buildAllBaseDeepCopiesArrays() {
        var config = new RPGMobsConfig();
        var resolved = new ResolvedConfig();

        OverlayFieldRegistry.buildAllBase(config, resolved);

        float originalFirst = resolved.healthMultiplierPerTier[0];
        config.healthConfig.mobHealthMultiplierPerTier[0] = 999f;

        assertEquals(originalFirst, resolved.healthMultiplierPerTier[0]);
        assertNotEquals(999f, resolved.healthMultiplierPerTier[0]);
    }

    @Test
    void applyAllYamlSetsOverlayFromMap() {
        var overlay = new ConfigOverlay();
        Map<String, Object> yaml = new HashMap<>();
        yaml.put("enabled", true);
        yaml.put("healthRandomVariance", 0.5);
        yaml.put("healthMultiplierPerTier", List.of(1.0, 2.0, 3.0, 4.0, 5.0));
        yaml.put("distancePerTier", 2000.0);
        yaml.put("vanillaDroplistExtraRollsPerTier", List.of(1, 2, 3, 4, 5));
        yaml.put("nameplateTierEnabled", List.of(false, true, false, true, false));
        yaml.put("nameplatePrefixPerTier", List.of("A", "B", "C", "D", "E"));
        yaml.put("nameplateMode", "SIMPLE");

        OverlayFieldRegistry.applyAllYaml(yaml, overlay);

        assertEquals(Boolean.TRUE, overlay.enabled);
        assertEquals(0.5f, overlay.healthRandomVariance);
        assertNotNull(overlay.healthMultiplierPerTier);
        assertEquals(5, overlay.healthMultiplierPerTier.length);
        assertEquals(1.0f, overlay.healthMultiplierPerTier[0]);
        assertEquals(5.0f, overlay.healthMultiplierPerTier[4]);
        assertEquals(2000.0, overlay.distancePerTier);
        assertNotNull(overlay.vanillaDroplistExtraRollsPerTier);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, overlay.vanillaDroplistExtraRollsPerTier);
        assertNotNull(overlay.nameplateTierEnabled);
        assertArrayEquals(new boolean[]{false, true, false, true, false}, overlay.nameplateTierEnabled);
        assertNotNull(overlay.nameplatePrefixPerTier);
        assertArrayEquals(new String[]{"A", "B", "C", "D", "E"}, overlay.nameplatePrefixPerTier);
        assertEquals("SIMPLE", overlay.nameplateMode);
    }

    @Test
    void applyAllYamlLeavesNullForMissingKeys() {
        var overlay = new ConfigOverlay();
        Map<String, Object> yaml = new HashMap<>();

        OverlayFieldRegistry.applyAllYaml(yaml, overlay);

        assertNull(overlay.enabled);
        assertNull(overlay.healthMultiplierPerTier);
        assertNull(overlay.healthRandomVariance);
        assertNull(overlay.distancePerTier);
        assertNull(overlay.vanillaDroplistExtraRollsPerTier);
        assertNull(overlay.nameplateTierEnabled);
        assertNull(overlay.nameplatePrefixPerTier);
        assertNull(overlay.nameplateMode);
        assertNull(overlay.enableModelScaling);
        assertNull(overlay.rpgLevelingEnabled);
    }

    @Test
    void applyAllYamlHandlesWrongTypes() {
        var overlay = new ConfigOverlay();
        Map<String, Object> yaml = new HashMap<>();
        yaml.put("enabled", "notaboolean");
        yaml.put("healthMultiplierPerTier", "notalist");
        yaml.put("vanillaDroplistExtraRollsPerTier", 42);
        yaml.put("nameplateTierEnabled", 123);

        OverlayFieldRegistry.applyAllYaml(yaml, overlay);

        assertNull(overlay.enabled);
        assertNull(overlay.healthMultiplierPerTier);
        assertNull(overlay.vanillaDroplistExtraRollsPerTier);
        assertNull(overlay.nameplateTierEnabled);
    }

    @Test
    void mergeAllUsesOverlayWhenNonNull() {
        var overlay = new ConfigOverlay();
        overlay.enabled = false;
        overlay.healthRandomVariance = 0.99f;
        overlay.distancePerTier = 5000.0;

        var base = new ResolvedConfig();
        base.enabled = true;
        base.healthRandomVariance = 0.01f;
        base.distancePerTier = 1000.0;

        var result = new ResolvedConfig();
        OverlayFieldRegistry.mergeAll(overlay, base, result);

        assertFalse(result.enabled);
        assertEquals(0.99f, result.healthRandomVariance);
        assertEquals(5000.0, result.distancePerTier);
    }

    @Test
    void mergeAllFallsBackToBaseWhenNull() {
        var overlay = new ConfigOverlay();
        var base = new ResolvedConfig();
        base.enabled = true;
        base.healthRandomVariance = 0.15f;
        base.distancePerTier = 3000.0;
        base.enableNameplates = false;
        base.nameplateMode = "SIMPLE";

        var result = new ResolvedConfig();
        OverlayFieldRegistry.mergeAll(overlay, base, result);

        assertTrue(result.enabled);
        assertEquals(0.15f, result.healthRandomVariance);
        assertEquals(3000.0, result.distancePerTier);
        assertFalse(result.enableNameplates);
        assertEquals("SIMPLE", result.nameplateMode);
    }

    @Test
    void mergeAllDeepCopiesArraysFromOverlay() {
        var overlay = new ConfigOverlay();
        overlay.healthMultiplierPerTier = new float[]{10f, 20f, 30f, 40f, 50f};

        var base = new ResolvedConfig();
        var result = new ResolvedConfig();
        OverlayFieldRegistry.mergeAll(overlay, base, result);

        assertArrayEquals(new float[]{10f, 20f, 30f, 40f, 50f}, result.healthMultiplierPerTier);

        overlay.healthMultiplierPerTier[0] = 999f;
        assertEquals(10f, result.healthMultiplierPerTier[0]);
    }

    @Test
    void writeAllOnlyWritesNonNullFields() {
        var overlay = new ConfigOverlay();
        overlay.enabled = true;
        overlay.healthRandomVariance = 0.5f;

        Map<String, Object> map = new LinkedHashMap<>();
        OverlayFieldRegistry.writeAll(overlay, map);

        assertEquals(2, map.size());
        assertTrue(map.containsKey("enabled"));
        assertTrue(map.containsKey("healthRandomVariance"));
    }

    @Test
    void writeAllOmitsNullFields() {
        var overlay = new ConfigOverlay();
        Map<String, Object> map = new LinkedHashMap<>();
        OverlayFieldRegistry.writeAll(overlay, map);

        assertTrue(map.isEmpty());
    }

    @Test
    void writeAllCastsFloatToDouble() {
        var overlay = new ConfigOverlay();
        overlay.healthRandomVariance = 1.5f;

        Map<String, Object> map = new LinkedHashMap<>();
        OverlayFieldRegistry.writeAll(overlay, map);

        Object value = map.get("healthRandomVariance");
        assertInstanceOf(Double.class, value);
        assertEquals(1.5, (Double) value, 0.0001);
    }

    @Test
    void writeAllWritesFloatArrayAsDoubleList() {
        var overlay = new ConfigOverlay();
        overlay.healthMultiplierPerTier = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f};

        Map<String, Object> map = new LinkedHashMap<>();
        OverlayFieldRegistry.writeAll(overlay, map);

        Object value = map.get("healthMultiplierPerTier");
        assertInstanceOf(List.class, value);
        @SuppressWarnings("unchecked")
        List<Double> list = (List<Double>) value;
        assertEquals(5, list.size());
        assertInstanceOf(Double.class, list.get(0));
        assertEquals(1.0, list.get(0), 0.0001);
        assertEquals(5.0, list.get(4), 0.0001);
    }

    @Test
    void writeAllWritesBooleanArrayAsBooleanList() {
        var overlay = new ConfigOverlay();
        overlay.nameplateTierEnabled = new boolean[]{true, false, true, false, true};

        Map<String, Object> map = new LinkedHashMap<>();
        OverlayFieldRegistry.writeAll(overlay, map);

        Object value = map.get("nameplateTierEnabled");
        assertInstanceOf(List.class, value);
        @SuppressWarnings("unchecked")
        List<Boolean> list = (List<Boolean>) value;
        assertEquals(5, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertTrue(list.get(2));
        assertFalse(list.get(3));
        assertTrue(list.get(4));
    }

    @Test
    void allEffectivelyEqualReturnsTrueForIdenticalOverlays() {
        var a = new ConfigOverlay();
        a.enabled = true;
        a.healthRandomVariance = 0.5f;

        var b = new ConfigOverlay();
        b.enabled = true;
        b.healthRandomVariance = 0.5f;

        var base = new ResolvedConfig();

        assertTrue(OverlayFieldRegistry.allEffectivelyEqual(a, b, base));
    }

    @Test
    void allEffectivelyEqualReturnsTrueWhenBothNull() {
        var a = new ConfigOverlay();
        var b = new ConfigOverlay();
        var base = new ResolvedConfig();

        assertTrue(OverlayFieldRegistry.allEffectivelyEqual(a, b, base));
    }

    @Test
    void allEffectivelyEqualReturnsFalseWhenOneDiffers() {
        var a = new ConfigOverlay();
        a.enabled = true;

        var b = new ConfigOverlay();
        b.enabled = false;

        var base = new ResolvedConfig();

        assertFalse(OverlayFieldRegistry.allEffectivelyEqual(a, b, base));
    }

    @Test
    void allEffectivelyEqualResolvesNullAgainstBase() {
        var a = new ConfigOverlay();
        a.enabled = true;

        var b = new ConfigOverlay();

        var base = new ResolvedConfig();
        base.enabled = true;

        assertTrue(OverlayFieldRegistry.allEffectivelyEqual(a, b, base));
    }

    @Test
    void roundTripBuildWriteApplyMerge() {
        var config = new RPGMobsConfig();
        var baseResolved = new ResolvedConfig();
        OverlayFieldRegistry.buildAllBase(config, baseResolved);

        var overlay = new ConfigOverlay();
        overlay.enabled = false;
        overlay.healthRandomVariance = 0.77f;
        overlay.distancePerTier = 4500.0;
        overlay.healthMultiplierPerTier = new float[]{9f, 8f, 7f, 6f, 5f};
        overlay.vanillaDroplistExtraRollsPerTier = new int[]{10, 20, 30, 40, 50};
        overlay.nameplateTierEnabled = new boolean[]{false, false, true, true, false};
        overlay.nameplatePrefixPerTier = new String[]{"X", "Y", "Z", "W", "V"};
        overlay.nameplateMode = "SIMPLE";
        overlay.dropWeaponChance = 0.99;
        overlay.xpBonusPerAbility = 7777.0;

        Map<String, Object> map = new LinkedHashMap<>();
        OverlayFieldRegistry.writeAll(overlay, map);

        var restoredOverlay = new ConfigOverlay();
        OverlayFieldRegistry.applyAllYaml(map, restoredOverlay);

        var result = new ResolvedConfig();
        OverlayFieldRegistry.mergeAll(restoredOverlay, baseResolved, result);

        assertFalse(result.enabled);
        assertEquals(0.77f, result.healthRandomVariance, 0.001f);
        assertEquals(4500.0, result.distancePerTier);
        assertArrayEquals(new float[]{9f, 8f, 7f, 6f, 5f}, result.healthMultiplierPerTier);
        assertArrayEquals(new int[]{10, 20, 30, 40, 50}, result.vanillaDroplistExtraRollsPerTier);
        assertArrayEquals(new boolean[]{false, false, true, true, false}, result.nameplateTierEnabled);
        assertArrayEquals(new String[]{"X", "Y", "Z", "W", "V"}, result.nameplatePrefixPerTier);
        assertEquals("SIMPLE", result.nameplateMode);
        assertEquals(0.99, result.dropWeaponChance);
        assertEquals(7777.0, result.xpBonusPerAbility);
    }

    @Test
    void globalCooldownRoundTrip() {
        var config = new RPGMobsConfig();
        var baseResolved = new ResolvedConfig();
        OverlayFieldRegistry.buildAllBase(config, baseResolved);

        assertEquals(config.abilitiesConfig.globalCooldownMinSeconds, baseResolved.globalCooldownMinSeconds);
        assertEquals(config.abilitiesConfig.globalCooldownMaxSeconds, baseResolved.globalCooldownMaxSeconds);

        var overlay = new ConfigOverlay();
        overlay.globalCooldownMinSeconds = 2.0f;
        overlay.globalCooldownMaxSeconds = 5.0f;

        Map<String, Object> map = new LinkedHashMap<>();
        OverlayFieldRegistry.writeAll(overlay, map);
        assertTrue(map.containsKey("globalCooldownMinSeconds"));
        assertTrue(map.containsKey("globalCooldownMaxSeconds"));
        assertInstanceOf(Double.class, map.get("globalCooldownMinSeconds"));
        assertEquals(2.0, (Double) map.get("globalCooldownMinSeconds"), 0.001);
        assertEquals(5.0, (Double) map.get("globalCooldownMaxSeconds"), 0.001);

        var restoredOverlay = new ConfigOverlay();
        OverlayFieldRegistry.applyAllYaml(map, restoredOverlay);
        assertEquals(2.0f, restoredOverlay.globalCooldownMinSeconds);
        assertEquals(5.0f, restoredOverlay.globalCooldownMaxSeconds);

        var result = new ResolvedConfig();
        OverlayFieldRegistry.mergeAll(restoredOverlay, baseResolved, result);
        assertEquals(2.0f, result.globalCooldownMinSeconds, 0.001f);
        assertEquals(5.0f, result.globalCooldownMaxSeconds, 0.001f);
    }

    @Test
    void globalCooldownNullFallsBackToBase() {
        var overlay = new ConfigOverlay();
        assertNull(overlay.globalCooldownMinSeconds);
        assertNull(overlay.globalCooldownMaxSeconds);

        var base = new ResolvedConfig();
        base.globalCooldownMinSeconds = 1.0f;
        base.globalCooldownMaxSeconds = 3.0f;

        var result = new ResolvedConfig();
        OverlayFieldRegistry.mergeAll(overlay, base, result);

        assertEquals(1.0f, result.globalCooldownMinSeconds);
        assertEquals(3.0f, result.globalCooldownMaxSeconds);
    }

    @Test
    void globalCooldownEffectivelyEqualWhenNullMatchesBase() {
        var a = new ConfigOverlay();
        a.globalCooldownMinSeconds = 1.0f;

        var b = new ConfigOverlay();

        var base = new ResolvedConfig();
        base.globalCooldownMinSeconds = 1.0f;

        assertTrue(OverlayFieldRegistry.allEffectivelyEqual(a, b, base));
    }

    @Test
    void eachFieldHasUniqueYamlKey() {
        var overlay = buildFullyPopulatedOverlay();
        Map<String, Object> map = new LinkedHashMap<>();
        OverlayFieldRegistry.writeAll(overlay, map);

        assertEquals(34, map.size());
    }

    @Test
    void fieldCountMatchesExpected() {
        var overlay = buildFullyPopulatedOverlay();
        Map<String, Object> map = new LinkedHashMap<>();
        OverlayFieldRegistry.writeAll(overlay, map);

        assertTrue(map.containsKey("enabled"));
        assertTrue(map.containsKey("distancePerTier"));
        assertTrue(map.containsKey("distanceBonusInterval"));
        assertTrue(map.containsKey("distanceHealthBonusPerInterval"));
        assertTrue(map.containsKey("distanceDamageBonusPerInterval"));
        assertTrue(map.containsKey("distanceHealthBonusCap"));
        assertTrue(map.containsKey("distanceDamageBonusCap"));
        assertTrue(map.containsKey("enableHealthScaling"));
        assertTrue(map.containsKey("healthMultiplierPerTier"));
        assertTrue(map.containsKey("healthRandomVariance"));
        assertTrue(map.containsKey("enableDamageScaling"));
        assertTrue(map.containsKey("damageMultiplierPerTier"));
        assertTrue(map.containsKey("damageRandomVariance"));
        assertTrue(map.containsKey("vanillaDroplistExtraRollsPerTier"));
        assertTrue(map.containsKey("dropWeaponChance"));
        assertTrue(map.containsKey("dropArmorPieceChance"));
        assertTrue(map.containsKey("dropOffhandItemChance"));
        assertTrue(map.containsKey("droppedGearDurabilityMin"));
        assertTrue(map.containsKey("droppedGearDurabilityMax"));
        assertTrue(map.containsKey("defaultLootTemplate"));
        assertTrue(map.containsKey("eliteFallDamageDisabled"));
        assertTrue(map.containsKey("enableNameplates"));
        assertTrue(map.containsKey("nameplateMode"));
        assertTrue(map.containsKey("nameplateTierEnabled"));
        assertTrue(map.containsKey("nameplatePrefixPerTier"));
        assertTrue(map.containsKey("enableModelScaling"));
        assertTrue(map.containsKey("modelScalePerTier"));
        assertTrue(map.containsKey("modelScaleVariance"));
        assertTrue(map.containsKey("rpgLevelingEnabled"));
        assertTrue(map.containsKey("xpMultiplierPerTier"));
        assertTrue(map.containsKey("xpBonusPerAbility"));
        assertTrue(map.containsKey("minionXPMultiplier"));
        assertTrue(map.containsKey("globalCooldownMinSeconds"));
        assertTrue(map.containsKey("globalCooldownMaxSeconds"));

        assertEquals(34, map.size());
    }

    private static ConfigOverlay buildFullyPopulatedOverlay() {
        var overlay = new ConfigOverlay();
        overlay.enabled = true;
        overlay.distancePerTier = 1000.0;
        overlay.distanceBonusInterval = 100.0;
        overlay.distanceHealthBonusPerInterval = 0.01f;
        overlay.distanceDamageBonusPerInterval = 0.005f;
        overlay.distanceHealthBonusCap = 0.5f;
        overlay.distanceDamageBonusCap = 0.5f;
        overlay.enableHealthScaling = true;
        overlay.healthMultiplierPerTier = new float[]{1f, 2f, 3f, 4f, 5f};
        overlay.healthRandomVariance = 0.05f;
        overlay.enableDamageScaling = true;
        overlay.damageMultiplierPerTier = new float[]{1f, 2f, 3f, 4f, 5f};
        overlay.damageRandomVariance = 0.05f;
        overlay.vanillaDroplistExtraRollsPerTier = new int[]{0, 1, 2, 3, 4};
        overlay.dropWeaponChance = 0.05;
        overlay.dropArmorPieceChance = 0.05;
        overlay.dropOffhandItemChance = 0.05;
        overlay.droppedGearDurabilityMin = 0.3;
        overlay.droppedGearDurabilityMax = 0.8;
        overlay.defaultLootTemplate = "default";
        overlay.eliteFallDamageDisabled = true;
        overlay.enableNameplates = true;
        overlay.nameplateMode = "RANKED_ROLE";
        overlay.nameplateTierEnabled = new boolean[]{true, true, true, true, true};
        overlay.nameplatePrefixPerTier = new String[]{"A", "B", "C", "D", "E"};
        overlay.enableModelScaling = true;
        overlay.modelScalePerTier = new float[]{1f, 2f, 3f, 4f, 5f};
        overlay.modelScaleVariance = 0.04f;
        overlay.rpgLevelingEnabled = true;
        overlay.xpMultiplierPerTier = new float[]{1f, 2f, 3f, 4f, 5f};
        overlay.xpBonusPerAbility = 2500.0;
        overlay.minionXPMultiplier = 0.05;
        overlay.globalCooldownMinSeconds = 1.5f;
        overlay.globalCooldownMaxSeconds = 4.0f;
        return overlay;
    }
}
