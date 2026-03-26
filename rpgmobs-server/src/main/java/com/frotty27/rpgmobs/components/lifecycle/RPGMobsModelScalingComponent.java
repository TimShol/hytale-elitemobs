package com.frotty27.rpgmobs.components.lifecycle;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.FloatCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class RPGMobsModelScalingComponent implements Component<EntityStore> {

    public boolean scaleApplied;

    public float appliedScale;

    public float configuredBaseScale;

    public transient boolean resyncVerified;

    private static final KeyedCodec<Boolean> K_SCALED_APPLIED = new KeyedCodec<>("ScaledApplied", new BooleanCodec());
    private static final KeyedCodec<Float> K_APPLIED_SCALE = new KeyedCodec<>("AppliedScale", new FloatCodec());
    private static final KeyedCodec<Float> K_CONFIGURED_BASE_SCALE = new KeyedCodec<>("ConfiguredBaseScale", new FloatCodec());

    public static final BuilderCodec<RPGMobsModelScalingComponent> CODEC = BuilderCodec.builder(
            RPGMobsModelScalingComponent.class,
            RPGMobsModelScalingComponent::new
    ).append(K_SCALED_APPLIED, (c, v) -> c.scaleApplied = v, c -> c.scaleApplied).add()
     .append(K_APPLIED_SCALE, (c, v) -> c.appliedScale = v, c -> c.appliedScale).add()
     .append(K_CONFIGURED_BASE_SCALE, (c, v) -> c.configuredBaseScale = v, c -> c.configuredBaseScale).add()
     .build();

    public RPGMobsModelScalingComponent() {
    }

    @Override
    public Component<EntityStore> clone() {
        RPGMobsModelScalingComponent c = new RPGMobsModelScalingComponent();
        c.scaleApplied = this.scaleApplied;
        c.appliedScale = this.appliedScale;
        c.configuredBaseScale = this.configuredBaseScale;
        return c;
    }
}
