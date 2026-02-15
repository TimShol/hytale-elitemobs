package com.frotty27.elitemobs.utils;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WeightedIndexSelectorTest {

    @Test
    void returnsZeroForEmptyOrNull() {
        assertTrue(WeightedIndexSelector.pickWeightedIndex(null, new Random(1)) >= 0);
        assertTrue(WeightedIndexSelector.pickWeightedIndex(new double[0], new Random(1)) >= 0);
    }

    @Test
    void ignoresNonPositiveWeights() {
        double[] weights = new double[]{0.0, -1.0, 2.5, 0.0};
        int picked = WeightedIndexSelector.pickWeightedIndex(weights, new Random(42));
        assertTrue(picked >= 0 && picked < weights.length);
    }

    @Test
    void singlePositiveWeightAlwaysPicked() {
        double[] weights = new double[]{0.0, 3.0, 0.0};
        for (int i = 0; i < 10; i++) {
            int picked = WeightedIndexSelector.pickWeightedIndex(weights, new Random(i));
            assertTrue(picked >= 0 && picked < weights.length);
        }
    }
}
