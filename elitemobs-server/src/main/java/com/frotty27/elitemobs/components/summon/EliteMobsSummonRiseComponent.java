package com.frotty27.elitemobs.components.summon;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsSummonRiseComponent implements Component<EntityStore> {

    public boolean applied;

    private static final KeyedCodec<Boolean> K_APPLIED =
            new KeyedCodec<>("Applied", new BooleanCodec());

    public static final BuilderCodec<EliteMobsSummonRiseComponent> CODEC =
            BuilderCodec.builder(EliteMobsSummonRiseComponent.class, EliteMobsSummonRiseComponent::new)
                    .append(K_APPLIED, (c, v) -> c.applied = v, c -> c.applied).add()
                    .build();

    public EliteMobsSummonRiseComponent() {}

    @Override
    public Component<EntityStore> clone() {
        EliteMobsSummonRiseComponent c = new EliteMobsSummonRiseComponent();
        c.applied = this.applied;
        return c;
    }
}
