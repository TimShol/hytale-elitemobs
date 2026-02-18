package com.frotty27.rpgmobs.components.data;

public record EffectState(long appliedTick, long durationTicks, int stackCount, boolean applied) {
}
