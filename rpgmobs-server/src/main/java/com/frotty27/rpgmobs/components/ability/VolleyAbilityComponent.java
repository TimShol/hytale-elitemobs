package com.frotty27.rpgmobs.components.ability;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.simple.BooleanCodec;
import com.hypixel.hytale.codec.codecs.simple.FloatCodec;
import com.hypixel.hytale.codec.codecs.simple.LongCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class VolleyAbilityComponent implements AbilityEnabledComponent, WeaponSwappable {

    public boolean abilityEnabled;

    public long cooldownTicksRemaining;

    public float volleyTriggerChance;

    public transient boolean swapActive;
    public transient byte swapSlot = -1;
    public transient @Nullable ItemStack swapPreviousItem;

    private static final KeyedCodec<Boolean> K_ABILITY_ENABLED = new KeyedCodec<>("AbilityEnabled", new BooleanCodec());
    private static final KeyedCodec<Long> K_COOLDOWN_TICKS_REMAINING = new KeyedCodec<>("CooldownTicksRemaining",
                                                                                        new LongCodec()
    );
    private static final KeyedCodec<Float> K_VOLLEY_TRIGGER_CHANCE = new KeyedCodec<>("VolleyTriggerChance", new FloatCodec());

    public static final BuilderCodec<VolleyAbilityComponent> CODEC = BuilderCodec.builder(VolleyAbilityComponent.class,
                                                                                           VolleyAbilityComponent::new
    ).append(K_ABILITY_ENABLED, (c, v) -> c.abilityEnabled = v, c -> c.abilityEnabled).add().append(
            K_COOLDOWN_TICKS_REMAINING,
            (c, v) -> c.cooldownTicksRemaining = v,
            c -> c.cooldownTicksRemaining
    ).add().append(
            K_VOLLEY_TRIGGER_CHANCE,
            (c, v) -> c.volleyTriggerChance = v,
            c -> c.volleyTriggerChance
    ).add().build();

    public VolleyAbilityComponent() {
        this.abilityEnabled = false;
        this.cooldownTicksRemaining = 0L;
        this.volleyTriggerChance = 0f;
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
    public boolean isSwapActive() { return swapActive; }

    @Override
    public void setSwapActive(boolean active) { this.swapActive = active; }

    @Override
    public byte getSwapSlot() { return swapSlot; }

    @Override
    public void setSwapSlot(byte slot) { this.swapSlot = slot; }

    @Override
    public @Nullable ItemStack getSwapPreviousItem() { return swapPreviousItem; }

    @Override
    public void setSwapPreviousItem(@Nullable ItemStack item) { this.swapPreviousItem = item; }

    @Override
    public Component<EntityStore> clone() {
        VolleyAbilityComponent c = new VolleyAbilityComponent();
        c.abilityEnabled = this.abilityEnabled;
        c.cooldownTicksRemaining = this.cooldownTicksRemaining;
        c.volleyTriggerChance = this.volleyTriggerChance;
        return c;
    }
}
