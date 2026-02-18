package com.frotty27.rpgmobs.components.summon;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class RPGMobsSummonMinionTrackingComponent implements Component<EntityStore> {

    public int summonedAliveCount;

    private static final KeyedCodec<Integer> K_SUMMONED_ALIVE_COUNT = new KeyedCodec<>("SummonedAliveCount",
                                                                                       new IntegerCodec()
    );

    public static final BuilderCodec<RPGMobsSummonMinionTrackingComponent> CODEC = BuilderCodec.builder(
            RPGMobsSummonMinionTrackingComponent.class,
            RPGMobsSummonMinionTrackingComponent::new
    ).append(K_SUMMONED_ALIVE_COUNT, (c, v) -> c.summonedAliveCount = v, c -> c.summonedAliveCount).add().build();

    public RPGMobsSummonMinionTrackingComponent() {
    }

    @Override
    public Component<EntityStore> clone() {
        RPGMobsSummonMinionTrackingComponent c = new RPGMobsSummonMinionTrackingComponent();
        c.summonedAliveCount = this.summonedAliveCount;
        return c;
    }

    public void decrementCount() {
        this.summonedAliveCount = Math.max(0, this.summonedAliveCount - 1);
    }

    public boolean canSummonMore(int maxSummons) {
        return summonedAliveCount < maxSummons;
    }
}
