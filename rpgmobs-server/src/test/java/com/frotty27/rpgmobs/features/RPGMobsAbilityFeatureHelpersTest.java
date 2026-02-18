package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RPGMobsAbilityFeatureHelpersTest {

    @Test
    void resolveSummonRoleIdentifierFallsBackToDefault() {
        RPGMobsConfig.SummonAbilityConfig cfg = new RPGMobsConfig.SummonAbilityConfig();
        cfg.roleIdentifiers.clear();
        assertEquals("default", RPGMobsAbilityFeatureHelpers.resolveSummonRoleIdentifier(cfg, "Skeleton"));
    }

    @Test
    void resolveSummonRoleIdentifierMatchesContains() {
        RPGMobsConfig.SummonAbilityConfig cfg = new RPGMobsConfig.SummonAbilityConfig();
        cfg.roleIdentifiers.clear();
        cfg.roleIdentifiers.add("Skeleton_Frost");
        assertEquals("Skeleton_Frost",
                     RPGMobsAbilityFeatureHelpers.resolveSummonRoleIdentifier(cfg, "Skeleton_Frost_Mage")
        );
    }
}
