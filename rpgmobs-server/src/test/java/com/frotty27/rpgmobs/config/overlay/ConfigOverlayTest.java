package com.frotty27.rpgmobs.config.overlay;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigOverlayTest {

    @Test
    void effectivelyEqualsBothNullFieldsResolveToBase() {
        var a = new ConfigOverlay();
        var b = new ConfigOverlay();
        var base = new ResolvedConfig();

        assertTrue(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsSameValuesReturnsTrue() {
        var a = new ConfigOverlay();
        a.enabled = true;
        a.healthRandomVariance = 0.5f;

        var b = new ConfigOverlay();
        b.enabled = true;
        b.healthRandomVariance = 0.5f;

        var base = new ResolvedConfig();

        assertTrue(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsDifferentEnabledReturnsFalse() {
        var a = new ConfigOverlay();
        a.enabled = true;

        var b = new ConfigOverlay();
        b.enabled = false;

        var base = new ResolvedConfig();

        assertFalse(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsNullVsBaseValueReturnsTrue() {
        var a = new ConfigOverlay();
        a.enabled = null;

        var b = new ConfigOverlay();
        b.enabled = true;

        var base = new ResolvedConfig();
        base.enabled = true;

        assertTrue(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsNullVsDifferentReturnsFalse() {
        var a = new ConfigOverlay();
        a.enabled = null;

        var b = new ConfigOverlay();
        b.enabled = false;

        var base = new ResolvedConfig();
        base.enabled = true;

        assertFalse(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsArrayComparisonDeep() {
        var a = new ConfigOverlay();
        a.healthMultiplierPerTier = new float[]{1f, 2f, 3f, 4f, 5f};

        var b = new ConfigOverlay();
        b.healthMultiplierPerTier = new float[]{1f, 2f, 3f, 4f, 6f};

        var base = new ResolvedConfig();

        assertFalse(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsNullArraysResolveToBase() {
        var a = new ConfigOverlay();
        a.healthMultiplierPerTier = null;

        var b = new ConfigOverlay();
        b.healthMultiplierPerTier = null;

        var base = new ResolvedConfig();
        base.healthMultiplierPerTier = new float[]{0.3f, 0.6f, 1.2f, 1.8f, 2.6f};

        assertTrue(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsProgressionStyleResolution() {
        var a = new ConfigOverlay();
        a.progressionStyle = null;

        var b = new ConfigOverlay();
        b.progressionStyle = "ENVIRONMENT";

        var base = new ResolvedConfig();
        base.progressionStyle = RPGMobsConfig.ProgressionStyle.ENVIRONMENT;

        assertTrue(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsSpawnChanceArrays() {
        double[] baseChances = {0.46, 0.28, 0.16, 0.08, 0.04};

        var a = new ConfigOverlay();
        a.spawnChancePerTier = null;

        var b = new ConfigOverlay();
        b.spawnChancePerTier = new double[]{0.46, 0.28, 0.16, 0.08, 0.04};

        var base = new ResolvedConfig();
        base.spawnChancePerTier = baseChances;

        assertTrue(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsEnvironmentTierRulesMatching() {
        Map<String, double[]> envA = new LinkedHashMap<>();
        envA.put("zone0", new double[]{100, 0, 0, 0, 0});

        Map<String, double[]> envB = new LinkedHashMap<>();
        envB.put("zone0", new double[]{100, 0, 0, 0, 0});

        var a = new ConfigOverlay();
        a.environmentTierRules = envA;

        var b = new ConfigOverlay();
        b.environmentTierRules = envB;

        var base = new ResolvedConfig();

        assertTrue(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsEnvironmentTierRulesDifferent() {
        Map<String, double[]> envA = new LinkedHashMap<>();
        envA.put("zone0", new double[]{100, 0, 0, 0, 0});

        Map<String, double[]> envB = new LinkedHashMap<>();
        envB.put("zone0", new double[]{50, 50, 0, 0, 0});

        var a = new ConfigOverlay();
        a.environmentTierRules = envA;

        var b = new ConfigOverlay();
        b.environmentTierRules = envB;

        var base = new ResolvedConfig();

        assertFalse(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void effectivelyEqualsTierOverridesDifference() {
        var a = new ConfigOverlay();
        a.tierOverrides = new LinkedHashMap<>();
        var to = new ConfigOverlay.TierOverride();
        to.allowedTiers = new boolean[]{true, false, false, false, false};
        a.tierOverrides.put("zombie", to);

        var b = new ConfigOverlay();
        b.tierOverrides = null;

        var base = new ResolvedConfig();

        assertFalse(ConfigOverlay.effectivelyEquals(a, b, base));
    }

    @Test
    void abilityLinkedEntryEqualsAndHashCode() {
        var entry1 = new ConfigOverlay.AbilityLinkedEntry("zombie", new boolean[]{true, true, false, false, true});
        var entry2 = new ConfigOverlay.AbilityLinkedEntry("zombie", new boolean[]{true, true, false, false, true});
        var entry3 = new ConfigOverlay.AbilityLinkedEntry("skeleton", new boolean[]{true, true, false, false, true});
        var entry4 = new ConfigOverlay.AbilityLinkedEntry("zombie", new boolean[]{false, true, false, false, true});

        assertEquals(entry1, entry2);
        assertEquals(entry1.hashCode(), entry2.hashCode());
        assertNotEquals(entry1, entry3);
        assertNotEquals(entry1, entry4);
    }

    @Test
    void tierOverrideEqualsAndHashCode() {
        var to1 = new ConfigOverlay.TierOverride();
        to1.allowedTiers = new boolean[]{true, false, true, false, true};

        var to2 = new ConfigOverlay.TierOverride();
        to2.allowedTiers = new boolean[]{true, false, true, false, true};

        var to3 = new ConfigOverlay.TierOverride();
        to3.allowedTiers = new boolean[]{false, false, true, false, true};

        assertEquals(to1, to2);
        assertEquals(to1.hashCode(), to2.hashCode());
        assertNotEquals(to1, to3);
    }
}
