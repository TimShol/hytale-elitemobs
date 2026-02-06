package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.config.EliteMobsConfig;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EliteMobsAbilityInteractionHelperTest {

    @Test
    void rollHealTriggerPercentHonorsConfiguredRange() {
        EliteMobsConfig.HealAbilityConfig cfg = new EliteMobsConfig.HealAbilityConfig();
        cfg.minHealthTriggerPercent = 0.2f;
        cfg.maxHealthTriggerPercent = 0.35f;

        Random random = new Random(42);
        for (int i = 0; i < 50; i++) {
            float value = EliteMobsAbilityInteractionHelper.rollHealTriggerPercent(random, cfg);
            assertTrue(value >= 0.2f && value <= 0.35f);
        }
    }
}
