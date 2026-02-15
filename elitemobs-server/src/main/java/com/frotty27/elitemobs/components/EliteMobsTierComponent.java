package com.frotty27.elitemobs.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class EliteMobsTierComponent implements Component<EntityStore> {

    public int tierIndex = -1;
    public boolean disableDrops;

    private static final KeyedCodec<Integer> K_TIER = new KeyedCodec<>("TierIndex", new IntegerCodec());
    private static final KeyedCodec<Boolean> K_DISABLE_DROPS = new KeyedCodec<>("DisableDrops", new BooleanCodec());

    public static final BuilderCodec<EliteMobsTierComponent> CODEC =
        BuilderCodec.builder(EliteMobsTierComponent.class, EliteMobsTierComponent::new)
            .append(K_TIER, (c, v) -> c.tierIndex = v, c -> c.tierIndex).add()
            .append(K_DISABLE_DROPS, (c, v) -> c.disableDrops = v, c -> c.disableDrops).add()
            .build();

    @Override
    public Component<EntityStore> clone() {
        EliteMobsTierComponent c = new EliteMobsTierComponent();
        c.tierIndex = this.tierIndex;
        c.disableDrops = this.disableDrops;
        return c;
    }
}
