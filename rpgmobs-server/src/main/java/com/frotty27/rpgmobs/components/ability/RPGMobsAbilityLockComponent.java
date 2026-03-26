package com.frotty27.rpgmobs.components.ability;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsAbilityLockComponent implements Component<EntityStore> {

    public @Nullable String activeAbilityId;

    public boolean chainStartPending;

    public long chainStartedAtTick = Long.MIN_VALUE;

    public long lockedAtTick = Long.MIN_VALUE;

    public long globalCooldownTicksRemaining;

    public int marker = 1;

    public static final int CHAIN_START_GRACE_TICKS = 5;

    private static final KeyedCodec<Integer> K_MARKER = new KeyedCodec<>("Marker", new IntegerCodec());
    private static final KeyedCodec<String> K_ACTIVE_ABILITY_ID = new KeyedCodec<>("ActiveAbilityId", new StringCodec());
    private static final KeyedCodec<Boolean> K_CHAIN_START_PENDING = new KeyedCodec<>("ChainStartPending", new BooleanCodec());
    private static final KeyedCodec<Long> K_CHAIN_STARTED_AT_TICK = new KeyedCodec<>("ChainStartedAtTick", new LongCodec());
    private static final KeyedCodec<Long> K_LOCKED_AT_TICK = new KeyedCodec<>("LockedAtTick", new LongCodec());
    private static final KeyedCodec<Long> K_GLOBAL_COOLDOWN_TICKS_REMAINING = new KeyedCodec<>("GlobalCooldownTicksRemaining", new LongCodec());

    public static final BuilderCodec<RPGMobsAbilityLockComponent> CODEC = BuilderCodec.builder(
            RPGMobsAbilityLockComponent.class,
            RPGMobsAbilityLockComponent::new
    ).append(K_MARKER, (c, v) -> c.marker = v, c -> c.marker).add()
     .append(K_ACTIVE_ABILITY_ID, (c, v) -> c.activeAbilityId = (v != null && !v.isEmpty()) ? v : null, c -> c.activeAbilityId != null ? c.activeAbilityId : "").add()
     .append(K_CHAIN_START_PENDING, (c, v) -> c.chainStartPending = v, c -> c.chainStartPending).add()
     .append(K_CHAIN_STARTED_AT_TICK, (c, v) -> c.chainStartedAtTick = v, c -> c.chainStartedAtTick).add()
     .append(K_LOCKED_AT_TICK, (c, v) -> c.lockedAtTick = v, c -> c.lockedAtTick).add()
     .append(K_GLOBAL_COOLDOWN_TICKS_REMAINING, (c, v) -> c.globalCooldownTicksRemaining = v, c -> c.globalCooldownTicksRemaining).add()
     .build();

    public RPGMobsAbilityLockComponent() {
        this.activeAbilityId = null;
        this.chainStartPending = false;
        this.globalCooldownTicksRemaining = 0L;
    }

    @Override
    public Component<EntityStore> clone() {
        RPGMobsAbilityLockComponent c = new RPGMobsAbilityLockComponent();
        c.activeAbilityId = this.activeAbilityId;
        c.chainStartPending = this.chainStartPending;
        c.chainStartedAtTick = this.chainStartedAtTick;
        c.lockedAtTick = this.lockedAtTick;
        c.globalCooldownTicksRemaining = this.globalCooldownTicksRemaining;
        return c;
    }

    public boolean isLocked() {
        return activeAbilityId != null || globalCooldownTicksRemaining > 0;
    }

    public boolean isOnGlobalCooldown() {
        return globalCooldownTicksRemaining > 0;
    }

    public boolean isChainStartPending() {
        return chainStartPending;
    }

    public boolean isWithinStartGracePeriod(long currentTick) {
        if (chainStartedAtTick == Long.MIN_VALUE) return false;
        return (currentTick - chainStartedAtTick) < CHAIN_START_GRACE_TICKS;
    }

    public void lock(String abilityId) {
        this.activeAbilityId = abilityId;
        this.chainStartPending = true;
        this.chainStartedAtTick = Long.MIN_VALUE;
    }

    public void lockWithTimestamp(String abilityId, long currentTick) {
        this.activeAbilityId = abilityId;
        this.chainStartPending = true;
        this.chainStartedAtTick = Long.MIN_VALUE;
        this.lockedAtTick = currentTick;
    }

    public void unlock() {
        this.activeAbilityId = null;
        this.chainStartPending = false;
        this.chainStartedAtTick = Long.MIN_VALUE;
        this.lockedAtTick = Long.MIN_VALUE;
    }

    public void unlockWithGlobalCooldown(float minSeconds, float maxSeconds) {
        this.activeAbilityId = null;
        this.chainStartPending = false;
        this.chainStartedAtTick = Long.MIN_VALUE;
        this.lockedAtTick = Long.MIN_VALUE;
        float randomSeconds = minSeconds + (float) (Math.random() * (maxSeconds - minSeconds));
        this.globalCooldownTicksRemaining = (long) (randomSeconds * 30);
    }

    public boolean isLockedBeyondTimeout(long currentTick, long maxTicks) {
        if (lockedAtTick == Long.MIN_VALUE || activeAbilityId == null) return false;
        return (currentTick - lockedAtTick) > maxTicks;
    }

    public void markChainStarted(long currentTick) {
        this.chainStartPending = false;
        this.chainStartedAtTick = currentTick;
    }
}
