package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.features.EliteMobsAbilityFeatureHelpers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EliteMobsAbilityFeatureHelpersTest {

    @Test
    void resolveSummonRoleIdentifierFallsBackToDefault() {
        EliteMobsConfig.SummonAbilityConfig cfg = new EliteMobsConfig.SummonAbilityConfig();
        cfg.roleIdentifiers.clear();
        assertEquals("default", EliteMobsAbilityFeatureHelpers.resolveSummonRoleIdentifier(cfg, "Skeleton"));
    }

    @Test
    void resolveSummonRoleIdentifierMatchesContains() {
        EliteMobsConfig.SummonAbilityConfig cfg = new EliteMobsConfig.SummonAbilityConfig();
        cfg.roleIdentifiers.clear();
        cfg.roleIdentifiers.add("Skeleton_Frost");
        assertEquals("Skeleton_Frost", EliteMobsAbilityFeatureHelpers.resolveSummonRoleIdentifier(cfg, "Skeleton_Frost_Mage"));
    }
}
