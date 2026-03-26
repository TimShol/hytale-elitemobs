package com.frotty27.rpgmobs.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalAbilityConfigTest {

    @Test
    void summonMinCountLessThanOrEqualToMaxCount() {
        var summonConfig = new RPGMobsConfig.SummonAbilityConfig();
        assertTrue(summonConfig.summonMinCount <= summonConfig.summonMaxCount,
                "summonMinCount (%d) should be <= summonMaxCount (%d)"
                        .formatted(summonConfig.summonMinCount, summonConfig.summonMaxCount));
    }

    @Test
    void minionMinTierLessThanOrEqualToMaxTier() {
        var summonConfig = new RPGMobsConfig.SummonAbilityConfig();
        assertTrue(summonConfig.minionMinTier <= summonConfig.minionMaxTier,
                "minionMinTier (%d) should be <= minionMaxTier (%d)"
                        .formatted(summonConfig.minionMinTier, summonConfig.minionMaxTier));
    }

    @Test
    void minionTiersWithinValidRange() {
        var summonConfig = new RPGMobsConfig.SummonAbilityConfig();
        assertTrue(summonConfig.minionMinTier >= 0 && summonConfig.minionMinTier <= 4);
        assertTrue(summonConfig.minionMaxTier >= 0 && summonConfig.minionMaxTier <= 4);
    }

    @Test
    void globalCooldownMinLessThanOrEqualToMax() {
        var config = new RPGMobsConfig();
        assertTrue(config.abilitiesConfig.globalCooldownMinSeconds <= config.abilitiesConfig.globalCooldownMaxSeconds,
                "globalCooldownMinSeconds (%.1f) should be <= globalCooldownMaxSeconds (%.1f)"
                        .formatted(config.abilitiesConfig.globalCooldownMinSeconds,
                                config.abilitiesConfig.globalCooldownMaxSeconds));
    }

    @Test
    void chargedAttackDodgeMultiplierDefaultIsPositive() {
        var dodgeConfig = new RPGMobsConfig.DodgeRollAbilityConfig();
        assertTrue(dodgeConfig.chargedAttackDodgeMultiplier > 0f);
    }

    @Test
    void interruptHitCountDefaultIsPositive() {
        var healConfig = new RPGMobsConfig.HealLeapAbilityConfig();
        assertTrue(healConfig.interruptHitCount > 0);
    }

    @Test
    void summonSpawnRadiusDefaultIsPositive() {
        var summonConfig = new RPGMobsConfig.SummonAbilityConfig();
        assertTrue(summonConfig.summonSpawnRadius > 0.0);
    }
}
