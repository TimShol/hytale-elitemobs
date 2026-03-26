package com.frotty27.rpgmobs.components.ability;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.FloatCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.codec.codecs.simple.StringCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class MultiSlashShortComponent implements AbilityEnabledComponent {

    public boolean abilityEnabled;
    public long cooldownTicksRemaining;
    public float slashTriggerChance;
    public String weaponVariant = "swords";

    private static final KeyedCodec<Boolean> K_ABILITY_ENABLED = new KeyedCodec<>("AbilityEnabled", new BooleanCodec());
    private static final KeyedCodec<Long> K_COOLDOWN_TICKS_REMAINING = new KeyedCodec<>("CooldownTicksRemaining", new LongCodec());
    private static final KeyedCodec<String> K_WEAPON_VARIANT = new KeyedCodec<>("WeaponVariant", new StringCodec());
    private static final KeyedCodec<Float> K_SLASH_TRIGGER_CHANCE = new KeyedCodec<>("SlashTriggerChance", new FloatCodec());

    public static final BuilderCodec<MultiSlashShortComponent> CODEC = BuilderCodec.builder(MultiSlashShortComponent.class,
            MultiSlashShortComponent::new
    ).append(K_ABILITY_ENABLED, (c, v) -> c.abilityEnabled = v, c -> c.abilityEnabled).add().append(
            K_COOLDOWN_TICKS_REMAINING,
            (c, v) -> c.cooldownTicksRemaining = v,
            c -> c.cooldownTicksRemaining
    ).add().append(
            K_WEAPON_VARIANT,
            (c, v) -> c.weaponVariant = v,
            c -> c.weaponVariant
    ).add().append(
            K_SLASH_TRIGGER_CHANCE,
            (c, v) -> c.slashTriggerChance = v,
            c -> c.slashTriggerChance
    ).add().build();

    public MultiSlashShortComponent() {
        this.abilityEnabled = false;
        this.cooldownTicksRemaining = 0L;
        this.slashTriggerChance = 0f;
        this.weaponVariant = "swords";
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
        MultiSlashShortComponent c = new MultiSlashShortComponent();
        c.abilityEnabled = this.abilityEnabled;
        c.cooldownTicksRemaining = this.cooldownTicksRemaining;
        c.slashTriggerChance = this.slashTriggerChance;
        c.weaponVariant = this.weaponVariant;
        return c;
    }
}
