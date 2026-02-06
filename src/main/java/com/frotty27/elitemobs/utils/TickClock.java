package com.frotty27.elitemobs.utils;

import static com.frotty27.elitemobs.utils.Constants.TICKS_PER_SECOND;

public final class TickClock {

    private long currentTick = 0L;

    private float accumulatedSeconds = 0f;

    public long getTick() {
        return currentTick;
    }

    public void advance(float deltaTimeSeconds) {
        if (deltaTimeSeconds <= 0f) return;

        accumulatedSeconds += deltaTimeSeconds;

        final float secondsPerTick = 1.0f / (float) TICKS_PER_SECOND;

        while (accumulatedSeconds >= secondsPerTick) {
            accumulatedSeconds -= secondsPerTick;
            currentTick++;
        }
    }
}
