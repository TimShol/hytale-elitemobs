package com.frotty27.elitemobs.utils;

import java.util.Random;

public final class WeightedIndexSelector {

    private WeightedIndexSelector() {
    }

    public static int pickWeightedIndex(double[] weights, Random random) {
        if (weights == null || weights.length == 0) {
            return 0;
        }

        double totalWeight = 0.0;
        for (double weight : weights) {
            if (weight > 0.0) {
                totalWeight += weight;
            }
        }

        if (totalWeight <= 0.0) {
            return 0;
        }

        double remainingWeight = random.nextDouble() * totalWeight;

        for (int index = 0; index < weights.length; index++) {
            double weight = weights[index];
            if (weight <= 0.0) continue;

            remainingWeight -= weight;
            if (remainingWeight <= 0.0) {
                return index;
            }
        }

        return weights.length - 1;
    }
}
