package com.frotty27.elitemobs.utils;

import static com.frotty27.elitemobs.utils.Constants.*;

public final class ClampingHelpers {

    private ClampingHelpers() {
    }

    public static int clampTierIndex(int tierIndex) {
        return Math.max(TIER_MIN, Math.min(TIER_MAX, tierIndex));
    }

    public static int clampArmorSlots(int requestedSlots) {
        return Math.max(MIN_ARMOR_SLOTS, Math.min(MAX_ARMOR_SLOTS, requestedSlots));
    }

    public static int clampInt(int value, int lowerBound, int upperBound) {
        if (upperBound < lowerBound) return lowerBound;
        if (value < lowerBound) return lowerBound;
        return Math.min(value, upperBound);
    }

    public static double clampDouble(double value) {
        if (Double.isNaN(value)) return 0.0;
        if (value < 0.0) return 0.0;
        return Math.min(value, 1.0);
    }

    public static double clampDouble(double value, double lowerBound, double upperBound) {
        if (value < lowerBound) return lowerBound;
        return Math.min(value, upperBound);
    }

    public static float clampFloat(float value, float lowerBound, float upperBound) {
        if (value < lowerBound) return lowerBound;
        return Math.min(value, upperBound);
    }

    public static float clamp01(float value) {
        if (Float.isNaN(value)) return 0f;
        if (value < 0f) return 0f;
        return Math.min(value, 1f);
    }
}
