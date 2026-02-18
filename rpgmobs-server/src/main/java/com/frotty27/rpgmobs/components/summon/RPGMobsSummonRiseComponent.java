package com.frotty27.rpgmobs.components.summon;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class RPGMobsSummonRiseComponent implements Component<EntityStore> {

    public boolean applied;

    private static final KeyedCodec<Boolean> K_APPLIED = new KeyedCodec<>("Applied", new BooleanCodec());

    public static final BuilderCodec<RPGMobsSummonRiseComponent> CODEC = BuilderCodec.builder(RPGMobsSummonRiseComponent.class,
                                                                                              RPGMobsSummonRiseComponent::new
    ).append(K_APPLIED, (c, v) -> c.applied = v, c -> c.applied).add().build();

    public RPGMobsSummonRiseComponent() {
    }

    @Override
    public Component<EntityStore> clone() {
        RPGMobsSummonRiseComponent c = new RPGMobsSummonRiseComponent();
        c.applied = this.applied;
        return c;
    }
}
