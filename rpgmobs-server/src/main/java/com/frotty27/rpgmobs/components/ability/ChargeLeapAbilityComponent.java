package com.frotty27.rpgmobs.components.ability;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ChargeLeapAbilityComponent implements Component<EntityStore> {

    public boolean abilityEnabled;

    public long cooldownTicksRemaining;

    private static final KeyedCodec<Boolean> K_ABILITY_ENABLED = new KeyedCodec<>("AbilityEnabled", new BooleanCodec());
    private static final KeyedCodec<Long> K_COOLDOWN_TICKS_REMAINING = new KeyedCodec<>("CooldownTicksRemaining",
                                                                                        new LongCodec()
    );

    public static final BuilderCodec<ChargeLeapAbilityComponent> CODEC = BuilderCodec.builder(ChargeLeapAbilityComponent.class,
                                                                                              ChargeLeapAbilityComponent::new
    ).append(K_ABILITY_ENABLED, (c, v) -> c.abilityEnabled = v, c -> c.abilityEnabled).add().append(
            K_COOLDOWN_TICKS_REMAINING,
            (c, v) -> c.cooldownTicksRemaining = v,
            c -> c.cooldownTicksRemaining
    ).add().build();

    public ChargeLeapAbilityComponent() {
        this.abilityEnabled = false;
        this.cooldownTicksRemaining = 0L;
    }

    @Override
    public Component<EntityStore> clone() {
        ChargeLeapAbilityComponent c = new ChargeLeapAbilityComponent();
        c.abilityEnabled = this.abilityEnabled;
        c.cooldownTicksRemaining = this.cooldownTicksRemaining;
        return c;
    }
}
