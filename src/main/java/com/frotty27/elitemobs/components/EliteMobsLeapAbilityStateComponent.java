package com.frotty27.elitemobs.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.FloatCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsLeapAbilityStateComponent implements Component<EntityStore> {

    public float cooldownSeconds;
    public float timeSinceLastAbilityCheckSeconds;
    public int abilityCheckStepCounter;
    public float debugSampleTimeLeft;

    private static final KeyedCodec<Float> K_COOLDOWN_SECONDS =
            new KeyedCodec<>("CooldownSeconds", new FloatCodec());

    private static final KeyedCodec<Float> K_TIME_SINCE_LAST_ABILITY_CHECK_SECONDS =
            new KeyedCodec<>("TimeSinceLastAbilityCheckSeconds", new FloatCodec());

    private static final KeyedCodec<Integer> K_ABILITY_CHECK_STEP_COUNTER =
            new KeyedCodec<>("AbilityCheckStepCounter", new IntegerCodec());

    private static final KeyedCodec<Float> K_DEBUG_SAMPLE_TIME_LEFT =
            new KeyedCodec<>("DebugSampleTimeLeft", new FloatCodec());

    public static final BuilderCodec<EliteMobsLeapAbilityStateComponent> CODEC =
            BuilderCodec.builder(EliteMobsLeapAbilityStateComponent.class, EliteMobsLeapAbilityStateComponent::new)
                    .append(K_COOLDOWN_SECONDS, (c, v) -> c.cooldownSeconds = v, c -> c.cooldownSeconds).add()
                    .append(K_TIME_SINCE_LAST_ABILITY_CHECK_SECONDS, (c, v) -> c.timeSinceLastAbilityCheckSeconds = v, c -> c.timeSinceLastAbilityCheckSeconds).add()
                    .append(K_ABILITY_CHECK_STEP_COUNTER, (c, v) -> c.abilityCheckStepCounter = v, c -> c.abilityCheckStepCounter).add()
                    .append(K_DEBUG_SAMPLE_TIME_LEFT, (c, v) -> c.debugSampleTimeLeft = v, c -> c.debugSampleTimeLeft).add()
                    .build();

    public EliteMobsLeapAbilityStateComponent() {}

    @Override
    public Component<EntityStore> clone() {
        EliteMobsLeapAbilityStateComponent c = new EliteMobsLeapAbilityStateComponent();
        c.cooldownSeconds = this.cooldownSeconds;
        c.timeSinceLastAbilityCheckSeconds = this.timeSinceLastAbilityCheckSeconds;
        c.abilityCheckStepCounter = this.abilityCheckStepCounter;
        c.debugSampleTimeLeft = this.debugSampleTimeLeft;
        return c;
    }
}