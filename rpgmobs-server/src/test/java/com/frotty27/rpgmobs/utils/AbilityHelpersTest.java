package com.frotty27.rpgmobs.utils;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbilityHelpersTest {

    @Test
    void rollPercentInRangeRespectsBounds() {
        Random random = new Random(123);
        for (int i = 0; i < 50; i++) {
            float value = AbilityHelpers.rollPercentInRange(random, 0.2f, 0.4f, 0.5f);
            assertTrue(value >= 0.2f && value <= 0.4f);
        }
    }

    @Test
    void rollPercentInRangeUsesFallbackWhenRandomMissing() {
        float value = AbilityHelpers.rollPercentInRange(null, 0.2f, 0.4f, 0.5f);
        assertEquals(0.5f, value);
    }
}
