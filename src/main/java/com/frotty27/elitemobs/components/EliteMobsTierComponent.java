package com.frotty27.elitemobs.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.FloatCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsTierComponent implements Component<EntityStore> {
    public int tierIndex = -1;

    public boolean scaledApplied;
    public float appliedScale;

    public boolean healthApplied;
    public float appliedHealthMult;
    public boolean healthFinalized;
    public int healthFinalizeTries;
    public float baseHealthMax;
    public float expectedHealthMax;
    public boolean needsHealthResync = true;

    public boolean projectileResistApplied;

    // ---- Leap ability state ----

    // ---- Reactive ability state ----
    public boolean healThresholdInitialized;
    public float healTriggerPercent;
    public long nextHealAllowedTick;
    public long nextSummonAllowedTick;
    public boolean healAbilityRollInitialized;
    public boolean healAbilityEnabledRoll;
    public boolean summonAbilityRollInitialized;
    public boolean summonAbilityEnabledRoll;
    public boolean healInProgress;
    public int healHitsTaken;
    // Runtime-only: not persisted.
    public boolean healSwapActive;
    public byte healSwapSlot = -1;
    public ItemStack healSwapPreviousItem;
    public long healSwapRestoreTick = -1;
    // Runtime-only summon scheduling.
    public long pendingSummonTick = -1;
    public String pendingSummonRoleIdentifier;
    public int summonedAliveCount;
    public boolean disableDrops;
    // Runtime-only target tracking.
    public Ref<EntityStore> lastAggroRef;
    public long lastAggroTick;
    // Runtime-only debug state
    public float debugSampleTimeLeft;

    // ----------------- PERSISTENCE CODEC -----------------

    private static final KeyedCodec<Integer> K_TIER = new KeyedCodec<>("TierIndex", new IntegerCodec());
    private static final KeyedCodec<Boolean> K_SCALED_APPLIED = new KeyedCodec<>("ScaledApplied", new BooleanCodec());
    private static final KeyedCodec<Float> K_APPLIED_SCALE = new KeyedCodec<>("AppliedScale", new FloatCodec());

    private static final KeyedCodec<Boolean> K_HEALTH_APPLIED = new KeyedCodec<>("HealthApplied", new BooleanCodec());
    private static final KeyedCodec<Float> K_APPLIED_HEALTH_MULT = new KeyedCodec<>("AppliedHealthMult",
                                                                                    new FloatCodec()
    );
    private static final KeyedCodec<Boolean> K_HEALTH_FINALIZED = new KeyedCodec<>("HealthFinalized",
                                                                                   new BooleanCodec()
    );
    private static final KeyedCodec<Integer> K_HEALTH_TRIES = new KeyedCodec<>("HealthFinalizeTries",
                                                                               new IntegerCodec()
    );
    private static final KeyedCodec<Float> K_BASE_HEALTH_MAX = new KeyedCodec<>("BaseHealthMax", new FloatCodec());
    private static final KeyedCodec<Float> K_EXPECTED_HEALTH_MAX = new KeyedCodec<>("ExpectedHealthMax",
                                                                                    new FloatCodec()
    );
    private static final KeyedCodec<Boolean> K_NEEDS_HEALTH_RESYNC = new KeyedCodec<>("NeedsHealthResync",
                                                                                      new BooleanCodec()
    );

    private static final KeyedCodec<Boolean> K_PROJ_RESIST_APPLIED = new KeyedCodec<>("ProjectileResistApplied",
                                                                                      new BooleanCodec()
    );

    private static final KeyedCodec<Boolean> K_HEAL_THRESHOLD_SET = new KeyedCodec<>("HealThresholdInitialized",
                                                                                     new BooleanCodec()
    );
    private static final KeyedCodec<Float> K_HEAL_TRIGGER = new KeyedCodec<>("HealTriggerPercent", new FloatCodec());
    private static final KeyedCodec<Long> K_NEXT_HEAL_TICK = new KeyedCodec<>("NextHealAllowedTick", new LongCodec());
    private static final KeyedCodec<Long> K_NEXT_SUMMON_TICK = new KeyedCodec<>("NextSummonAllowedTick",
                                                                                 new LongCodec()
    );
    private static final KeyedCodec<Boolean> K_HEAL_ROLL_INIT = new KeyedCodec<>("HealAbilityRollInitialized",
                                                                                 new BooleanCodec()
    );
    private static final KeyedCodec<Boolean> K_HEAL_ROLL = new KeyedCodec<>("HealAbilityEnabledRoll",
                                                                            new BooleanCodec()
    );
    private static final KeyedCodec<Boolean> K_SUMMON_ROLL_INIT = new KeyedCodec<>("SummonAbilityRollInitialized",
                                                                                   new BooleanCodec()
    );
    private static final KeyedCodec<Boolean> K_SUMMON_ROLL = new KeyedCodec<>("SummonAbilityEnabledRoll",
                                                                              new BooleanCodec()
    );
    private static final KeyedCodec<Boolean> K_HEAL_IN_PROGRESS = new KeyedCodec<>("HealInProgress",
                                                                                   new BooleanCodec()
    );
    private static final KeyedCodec<Integer> K_HEAL_HITS = new KeyedCodec<>("HealHitsTaken", new IntegerCodec());
    private static final KeyedCodec<Integer> K_SUMMON_ALIVE = new KeyedCodec<>("SummonedAliveCount",
                                                                               new IntegerCodec()
    );
    private static final KeyedCodec<Boolean> K_DISABLE_DROPS = new KeyedCodec<>("DisableDrops", new BooleanCodec());

    public static final BuilderCodec<EliteMobsTierComponent> CODEC = BuilderCodec.builder(EliteMobsTierComponent.class,
                                                                                          EliteMobsTierComponent::new
            ).append(K_TIER, (c, v) -> c.tierIndex = v, c -> c.tierIndex).add()

            .append(K_SCALED_APPLIED, (c, v) -> c.scaledApplied = v, c -> c.scaledApplied).add().append(K_APPLIED_SCALE,
                                                                                                        (c, v) -> c.appliedScale = v,
                                                                                                        c -> c.appliedScale
            ).add()

            .append(K_HEALTH_APPLIED, (c, v) -> c.healthApplied = v, c -> c.healthApplied).add().append(
                    K_APPLIED_HEALTH_MULT,
                    (c, v) -> c.appliedHealthMult = v,
                    c -> c.appliedHealthMult
            ).add().append(K_HEALTH_FINALIZED, (c, v) -> c.healthFinalized = v, c -> c.healthFinalized).add().append(
                    K_HEALTH_TRIES,
                    (c, v) -> c.healthFinalizeTries = v,
                    c -> c.healthFinalizeTries
            ).add().append(K_BASE_HEALTH_MAX, (c, v) -> c.baseHealthMax = v, c -> c.baseHealthMax).add().append(
                    K_EXPECTED_HEALTH_MAX,
                    (c, v) -> c.expectedHealthMax = v,
                    c -> c.expectedHealthMax
            ).add().append(K_NEEDS_HEALTH_RESYNC, (c, v) -> c.needsHealthResync = v, c -> c.needsHealthResync).add()

            .append(K_PROJ_RESIST_APPLIED,
                    (c, v) -> c.projectileResistApplied = v,
                    c -> c.projectileResistApplied
            ).add()

            .append(K_HEAL_THRESHOLD_SET, (c, v) -> c.healThresholdInitialized = v, c -> c.healThresholdInitialized)
            .add()
            .append(K_HEAL_TRIGGER, (c, v) -> c.healTriggerPercent = v, c -> c.healTriggerPercent)
            .add()
            .append(K_NEXT_HEAL_TICK, (c, v) -> c.nextHealAllowedTick = v, c -> c.nextHealAllowedTick)
            .add()
            .append(K_NEXT_SUMMON_TICK, (c, v) -> c.nextSummonAllowedTick = v, c -> c.nextSummonAllowedTick)
            .add()
            .append(K_HEAL_ROLL_INIT, (c, v) -> c.healAbilityRollInitialized = v, c -> c.healAbilityRollInitialized)
            .add()
            .append(K_HEAL_ROLL, (c, v) -> c.healAbilityEnabledRoll = v, c -> c.healAbilityEnabledRoll)
            .add()
            .append(K_SUMMON_ROLL_INIT,
                    (c, v) -> c.summonAbilityRollInitialized = v,
                    c -> c.summonAbilityRollInitialized
            ).add()
            .append(K_SUMMON_ROLL, (c, v) -> c.summonAbilityEnabledRoll = v, c -> c.summonAbilityEnabledRoll)
            .add()
            .append(K_HEAL_IN_PROGRESS, (c, v) -> c.healInProgress = v, c -> c.healInProgress)
            .add()
            .append(K_HEAL_HITS, (c, v) -> c.healHitsTaken = v, c -> c.healHitsTaken)
            .add()
            .append(K_SUMMON_ALIVE, (c, v) -> c.summonedAliveCount = v, c -> c.summonedAliveCount)
            .add()
            .append(K_DISABLE_DROPS, (c, v) -> c.disableDrops = v, c -> c.disableDrops)
            .add()

            .build();


    @Override
    public Component<EntityStore> clone() {
        EliteMobsTierComponent c = new EliteMobsTierComponent();
        c.tierIndex = this.tierIndex;

        c.scaledApplied = this.scaledApplied;
        c.appliedScale = this.appliedScale;

        c.healthApplied = this.healthApplied;
        c.appliedHealthMult = this.appliedHealthMult;
        c.healthFinalized = this.healthFinalized;
        c.healthFinalizeTries = this.healthFinalizeTries;
        c.baseHealthMax = this.baseHealthMax;
        c.expectedHealthMax = this.expectedHealthMax;
        c.needsHealthResync = this.needsHealthResync;

        c.projectileResistApplied = this.projectileResistApplied;

        c.healThresholdInitialized = this.healThresholdInitialized;
        c.healTriggerPercent = this.healTriggerPercent;
        c.nextHealAllowedTick = this.nextHealAllowedTick;
        c.nextSummonAllowedTick = this.nextSummonAllowedTick;
        c.healAbilityRollInitialized = this.healAbilityRollInitialized;
        c.healAbilityEnabledRoll = this.healAbilityEnabledRoll;
        c.summonAbilityRollInitialized = this.summonAbilityRollInitialized;
        c.summonAbilityEnabledRoll = this.summonAbilityEnabledRoll;
        c.healInProgress = this.healInProgress;
        c.healHitsTaken = this.healHitsTaken;
        c.healSwapActive = this.healSwapActive;
        c.healSwapSlot = this.healSwapSlot;
        c.healSwapPreviousItem = this.healSwapPreviousItem;
        c.healSwapRestoreTick = this.healSwapRestoreTick;
        c.pendingSummonTick = this.pendingSummonTick;
        c.pendingSummonRoleIdentifier = this.pendingSummonRoleIdentifier;
        c.summonedAliveCount = this.summonedAliveCount;
        c.disableDrops = this.disableDrops;
        c.lastAggroRef = this.lastAggroRef;
        c.lastAggroTick = this.lastAggroTick;
        c.debugSampleTimeLeft = this.debugSampleTimeLeft;

        return c;
    }
}
