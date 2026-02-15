package com.frotty27.elitemobs.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClampingHelpersTest {

    @Test
    void clampTierIndexBounds() {
        assertTrue(ClampingHelpers.clampTierIndex(-5) >= 0);
        assertTrue(ClampingHelpers.clampTierIndex(0) >= 0);
        assertTrue(ClampingHelpers.clampTierIndex(4) <= 4);
        assertTrue(ClampingHelpers.clampTierIndex(10) <= 4);
    }

    @Test
    void clampDoubleRange() {
        assertTrue(ClampingHelpers.clampDouble(-1.0) >= 0.0);
        assertTrue(ClampingHelpers.clampDouble(2.0) <= 1.0);
        assertTrue(ClampingHelpers.clampDouble(0.5) >= 0.0 && ClampingHelpers.clampDouble(0.5) <= 1.0);
    }
}
