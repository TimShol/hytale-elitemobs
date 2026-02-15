package com.frotty27.elitemobs.components.data;

public record EffectState(
    long appliedTick,
    long durationTicks,
    int stackCount,
    boolean applied
) {

    public boolean isExpired(long currentTick) {
        if (durationTicks < 0) {
            return false;
        }
        return currentTick >= appliedTick + durationTicks;
    }
}
