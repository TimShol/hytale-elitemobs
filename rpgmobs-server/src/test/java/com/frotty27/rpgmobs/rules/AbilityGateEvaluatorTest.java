package com.frotty27.rpgmobs.rules;

import com.frotty27.rpgmobs.config.RPGMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbilityGateEvaluatorTest {

    @Test
    void gateAllowsWhenNoRestrictions() {
        RPGMobsConfig.AbilityConfig cfg = new RPGMobsConfig.AbilityConfig();
        cfg.isEnabled = true;
        cfg.isEnabledPerTier = new boolean[]{true, true, true, true, true};
        cfg.gate = new RPGMobsConfig.AbilityGate();

        assertTrue(AbilityGateEvaluator.isAllowed(cfg, "goblin_scout", "weapon_sword", 2));
    }

    @Test
    void gateDeniesWhenRequiredMissing() {
        RPGMobsConfig.AbilityConfig cfg = new RPGMobsConfig.AbilityConfig();
        cfg.isEnabled = true;
        cfg.isEnabledPerTier = new boolean[]{true, true, true, true, true};

        RPGMobsConfig.AbilityGate gate = new RPGMobsConfig.AbilityGate();
        gate.weaponIdMustContain = List.of("sword");
        cfg.gate = gate;

        assertFalse(AbilityGateEvaluator.isAllowed(cfg, "goblin_scout", "weapon_staff", 1));
        assertTrue(AbilityGateEvaluator.isAllowed(cfg, "goblin_scout", "weapon_sword", 1));
    }

    @Test
    void rulesOverrideBaseGate() {
        RPGMobsConfig.AbilityConfig cfg = new RPGMobsConfig.AbilityConfig();
        cfg.isEnabled = true;
        cfg.isEnabledPerTier = new boolean[]{true, true, true, true, true};

        RPGMobsConfig.AbilityGate gate = new RPGMobsConfig.AbilityGate();
        gate.roleMustContain = List.of("goblin");

        RPGMobsConfig.AbilityRule denyRule = new RPGMobsConfig.AbilityRule();
        denyRule.enabled = true;
        denyRule.enabledPerTier = new boolean[]{true, true, true, true, true};
        denyRule.roleMustContain = List.of("goblin_boss");
        denyRule.deny = true;

        gate.rules = List.of(denyRule);
        cfg.gate = gate;

        assertTrue(AbilityGateEvaluator.isAllowed(cfg, "goblin_scout", "weapon_sword", 1));
        assertFalse(AbilityGateEvaluator.isAllowed(cfg, "goblin_boss", "weapon_sword", 1));
    }
}
