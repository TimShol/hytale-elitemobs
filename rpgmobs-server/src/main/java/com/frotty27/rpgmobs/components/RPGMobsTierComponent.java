package com.frotty27.rpgmobs.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.IntegerCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
public final class RPGMobsTierComponent implements Component<EntityStore> {

    public int tierIndex = -1;
    public String matchedRuleKey = "";

    public transient int lastReconciledAt = 0;

    private static final KeyedCodec<Integer> K_TIER = new KeyedCodec<>("TierIndex", new IntegerCodec());
    private static final KeyedCodec<String> K_MATCHED_RULE_KEY = new KeyedCodec<>("MatchedRuleKey", new StringCodec());

    public static final BuilderCodec<RPGMobsTierComponent> CODEC = BuilderCodec.builder(RPGMobsTierComponent.class,
                                                                                        RPGMobsTierComponent::new
    ).append(K_TIER, (c, v) -> c.tierIndex = v, c -> c.tierIndex).add()
     .append(K_MATCHED_RULE_KEY, (c, v) -> c.matchedRuleKey = v, c -> c.matchedRuleKey).add()
     .build();

    @Override
    public Component<EntityStore> clone() {
        RPGMobsTierComponent c = new RPGMobsTierComponent();
        c.tierIndex = this.tierIndex;
        c.matchedRuleKey = this.matchedRuleKey;
        return c;
    }
}
