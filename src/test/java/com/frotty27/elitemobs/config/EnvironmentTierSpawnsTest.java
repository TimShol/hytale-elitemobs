package com.frotty27.elitemobs.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnvironmentTierSpawnsTest {

    @Test
    void zone1DefaultsUseTier1To3Only() {
        EliteMobsConfig cfg = new EliteMobsConfig();
        EliteMobsConfig.EnvironmentTierRule rule = cfg.spawning.defaultEnvironmentTierSpawns.get("Env_Zone1_Forests");
        assertNotNull(rule);
        assertTrue(rule.spawnChancePerTier.length >= 5);
        assertTrue(rule.spawnChancePerTier[3] <= 0.0001);
        assertTrue(rule.spawnChancePerTier[4] <= 0.0001);
    }

    @Test
    void zone3DefaultsDisableTier1() {
        EliteMobsConfig cfg = new EliteMobsConfig();
        EliteMobsConfig.EnvironmentTierRule rule = cfg.spawning.defaultEnvironmentTierSpawns.get("Env_Zone3_Tundra");
        assertNotNull(rule);
        assertTrue(rule.spawnChancePerTier.length >= 5);
        assertTrue(rule.spawnChancePerTier[0] <= 0.0001);
    }

    @Test
    void zone4DefaultsPreferTier3() {
        EliteMobsConfig cfg = new EliteMobsConfig();
        EliteMobsConfig.EnvironmentTierRule rule = cfg.spawning.defaultEnvironmentTierSpawns.get("Env_Zone4_Wastes");
        assertNotNull(rule);
        assertTrue(rule.spawnChancePerTier.length >= 5);
        assertTrue(rule.spawnChancePerTier[0] <= 0.0001);
        assertTrue(rule.spawnChancePerTier[1] <= 0.0001);
        assertTrue(rule.spawnChancePerTier[2] >= rule.spawnChancePerTier[3]);
        assertTrue(rule.spawnChancePerTier[2] >= rule.spawnChancePerTier[4]);
    }
}
