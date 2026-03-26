package com.frotty27.rpgmobs.components.ability;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.FloatCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class DodgeRollAbilityComponent implements AbilityEnabledComponent {

    public boolean abilityEnabled;

    public long cooldownTicksRemaining;

    public float dodgeChance;

    private static final KeyedCodec<Boolean> K_ABILITY_ENABLED = new KeyedCodec<>("AbilityEnabled", new BooleanCodec());
    private static final KeyedCodec<Long> K_COOLDOWN_TICKS_REMAINING = new KeyedCodec<>("CooldownTicksRemaining",
                                                                                        new LongCodec()
    );
    private static final KeyedCodec<Float> K_DODGE_CHANCE = new KeyedCodec<>("DodgeChance", new FloatCodec());

    public static final BuilderCodec<DodgeRollAbilityComponent> CODEC = BuilderCodec.builder(DodgeRollAbilityComponent.class,
                                                                                              DodgeRollAbilityComponent::new
    ).append(K_ABILITY_ENABLED, (c, v) -> c.abilityEnabled = v, c -> c.abilityEnabled).add().append(
            K_COOLDOWN_TICKS_REMAINING,
            (c, v) -> c.cooldownTicksRemaining = v,
            c -> c.cooldownTicksRemaining
    ).add().append(
            K_DODGE_CHANCE,
            (c, v) -> c.dodgeChance = v,
            c -> c.dodgeChance
    ).add().build();

    public DodgeRollAbilityComponent() {
        this.abilityEnabled = false;
        this.cooldownTicksRemaining = 0L;
        this.dodgeChance = 0f;
    }

    @Override
    public boolean isAbilityEnabled() {
        return abilityEnabled;
    }

    @Override
    public void setAbilityEnabled(boolean enabled) {
        this.abilityEnabled = enabled;
    }

    @Override
    public long getCooldownTicksRemaining() {
        return cooldownTicksRemaining;
    }

    @Override
    public Component<EntityStore> clone() {
        DodgeRollAbilityComponent c = new DodgeRollAbilityComponent();
        c.abilityEnabled = this.abilityEnabled;
        c.cooldownTicksRemaining = this.cooldownTicksRemaining;
        c.dodgeChance = this.dodgeChance;
        return c;
    }
}
