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

public final class EnrageAbilityComponent implements AbilityEnabledComponent, WeaponSwappable {

    public boolean abilityEnabled;

    public long cooldownTicksRemaining;

    public float triggerHealthPercent;

    public boolean enraged;

    public transient boolean swapActive;
    public transient byte swapSlot = -1;
    public transient @Nullable ItemStack swapPreviousItem;

    public transient boolean utilitySwapActive;
    public transient @Nullable ItemStack utilitySwapPreviousItem;

    private static final KeyedCodec<Boolean> K_ABILITY_ENABLED = new KeyedCodec<>("AbilityEnabled", new BooleanCodec());
    private static final KeyedCodec<Long> K_COOLDOWN_TICKS_REMAINING = new KeyedCodec<>("CooldownTicksRemaining",
                                                                                        new LongCodec()
    );
    private static final KeyedCodec<Boolean> K_ENRAGED = new KeyedCodec<>("Enraged", new BooleanCodec());
    private static final KeyedCodec<Float> K_TRIGGER_HEALTH_PERCENT = new KeyedCodec<>("TriggerHealthPercent", new FloatCodec());

    public static final BuilderCodec<EnrageAbilityComponent> CODEC = BuilderCodec.builder(EnrageAbilityComponent.class,
                                                                                           EnrageAbilityComponent::new
    ).append(K_ABILITY_ENABLED, (c, v) -> c.abilityEnabled = v, c -> c.abilityEnabled).add().append(
            K_COOLDOWN_TICKS_REMAINING,
            (c, v) -> c.cooldownTicksRemaining = v,
            c -> c.cooldownTicksRemaining
    ).add().append(
            K_ENRAGED,
            (c, v) -> c.enraged = v,
            c -> c.enraged
    ).add().append(
            K_TRIGGER_HEALTH_PERCENT,
            (c, v) -> c.triggerHealthPercent = v,
            c -> c.triggerHealthPercent
    ).add().build();

    public EnrageAbilityComponent() {
        this.abilityEnabled = false;
        this.cooldownTicksRemaining = 0L;
        this.triggerHealthPercent = 0.3f;
        this.enraged = false;
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
        EnrageAbilityComponent c = new EnrageAbilityComponent();
        c.abilityEnabled = this.abilityEnabled;
        c.cooldownTicksRemaining = this.cooldownTicksRemaining;
        c.triggerHealthPercent = this.triggerHealthPercent;
        c.enraged = this.enraged;
        c.swapActive = this.swapActive;
        c.swapSlot = this.swapSlot;
        c.swapPreviousItem = this.swapPreviousItem;
        c.utilitySwapActive = this.utilitySwapActive;
        c.utilitySwapPreviousItem = this.utilitySwapPreviousItem;
        return c;
    }
}
