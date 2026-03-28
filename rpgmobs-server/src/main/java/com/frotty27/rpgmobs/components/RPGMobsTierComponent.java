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
    public String originalRoleName = "";

    public transient int lastReconciledAt = 0;
    public transient boolean pendingPostRoleChangeEquip = false;

    private static final KeyedCodec<Integer> K_TIER = new KeyedCodec<>("TierIndex", new IntegerCodec());
    private static final KeyedCodec<String> K_MATCHED_RULE_KEY = new KeyedCodec<>("MatchedRuleKey", new StringCodec());
    private static final KeyedCodec<String> K_ORIGINAL_ROLE_NAME = new KeyedCodec<>("OriginalRoleName", new StringCodec());

    public static final BuilderCodec<RPGMobsTierComponent> CODEC = BuilderCodec.builder(RPGMobsTierComponent.class,
                                                                                        RPGMobsTierComponent::new
    ).append(K_TIER, (c, v) -> c.tierIndex = v, c -> c.tierIndex).add()
     .append(K_MATCHED_RULE_KEY, (c, v) -> c.matchedRuleKey = v, c -> c.matchedRuleKey).add()
     .append(K_ORIGINAL_ROLE_NAME, (c, v) -> c.originalRoleName = v, c -> c.originalRoleName).add()
     .build();

    public String getEffectiveRoleName() {
        return originalRoleName != null && !originalRoleName.isEmpty() ? originalRoleName : matchedRuleKey;
    }

    @Override
    public Component<EntityStore> clone() {
        RPGMobsTierComponent c = new RPGMobsTierComponent();
        c.tierIndex = this.tierIndex;
        c.matchedRuleKey = this.matchedRuleKey;
        c.originalRoleName = this.originalRoleName;
        return c;
    }
}
