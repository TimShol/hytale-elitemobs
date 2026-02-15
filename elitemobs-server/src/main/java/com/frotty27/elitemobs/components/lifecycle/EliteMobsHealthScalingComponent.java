package com.frotty27.elitemobs.components.lifecycle;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.FloatCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsHealthScalingComponent implements Component<EntityStore> {

    public boolean healthApplied;
    public float appliedHealthMult;
    public float baseHealthMax;
    public boolean healthFinalized;

    public transient boolean resyncDone;
    public transient int healthFinalizeTries;

    private static final KeyedCodec<Boolean> K_HEALTH_APPLIED =
            new KeyedCodec<>("HealthApplied", new BooleanCodec());
    private static final KeyedCodec<Float> K_APPLIED_HEALTH_MULT =
            new KeyedCodec<>("AppliedHealthMult", new FloatCodec());
    private static final KeyedCodec<Float> K_BASE_HEALTH_MAX =
            new KeyedCodec<>("BaseHealthMax", new FloatCodec());
    private static final KeyedCodec<Boolean> K_HEALTH_FINALIZED =
            new KeyedCodec<>("HealthFinalized", new BooleanCodec());

    public static final BuilderCodec<EliteMobsHealthScalingComponent> CODEC =
            BuilderCodec.builder(EliteMobsHealthScalingComponent.class, EliteMobsHealthScalingComponent::new)
                    .append(K_HEALTH_APPLIED, (c, v) -> c.healthApplied = v, c -> c.healthApplied).add()
                    .append(K_APPLIED_HEALTH_MULT, (c, v) -> c.appliedHealthMult = v, c -> c.appliedHealthMult).add()
                    .append(K_BASE_HEALTH_MAX, (c, v) -> c.baseHealthMax = v, c -> c.baseHealthMax).add()
                    .append(K_HEALTH_FINALIZED, (c, v) -> c.healthFinalized = v, c -> c.healthFinalized).add()
                    .build();

    public EliteMobsHealthScalingComponent() {}

    @Override
    public Component<EntityStore> clone() {
        EliteMobsHealthScalingComponent c = new EliteMobsHealthScalingComponent();
        c.healthApplied = this.healthApplied;
        c.appliedHealthMult = this.appliedHealthMult;
        c.baseHealthMax = this.baseHealthMax;
        c.healthFinalized = this.healthFinalized;
        c.resyncDone = this.resyncDone;
        c.healthFinalizeTries = this.healthFinalizeTries;
        return c;
    }

    public boolean shouldRetryHealthFinalization() {
        return !healthFinalized && healthFinalizeTries < 5;
    }

    public void incrementFinalizeTries() {
        this.healthFinalizeTries++;
    }
}
