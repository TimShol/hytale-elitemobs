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

public final class HealLeapAbilityComponent implements Component<EntityStore> {

    public boolean abilityEnabled;
    public float triggerHealthPercent;

    public long cooldownTicksRemaining;

    public transient int hitsTaken;

    public transient boolean swapActive;
    public transient byte swapSlot;
    public transient @Nullable ItemStack swapPreviousItem;

    private static final KeyedCodec<Boolean> K_ABILITY_ENABLED = new KeyedCodec<>("AbilityEnabled", new BooleanCodec());
    private static final KeyedCodec<Float> K_TRIGGER_HEALTH_PERCENT = new KeyedCodec<>("TriggerHealthPercent",
                                                                                       new FloatCodec()
    );
    private static final KeyedCodec<Long> K_COOLDOWN_TICKS_REMAINING = new KeyedCodec<>("CooldownTicksRemaining",
                                                                                        new LongCodec()
    );

    public static final BuilderCodec<HealLeapAbilityComponent> CODEC = BuilderCodec.builder(HealLeapAbilityComponent.class,
                                                                                            HealLeapAbilityComponent::new
    ).append(K_ABILITY_ENABLED, (c, v) -> c.abilityEnabled = v, c -> c.abilityEnabled).add().append(
            K_TRIGGER_HEALTH_PERCENT,
            (c, v) -> c.triggerHealthPercent = v,
            c -> c.triggerHealthPercent
    ).add().append(K_COOLDOWN_TICKS_REMAINING,
                   (c, v) -> c.cooldownTicksRemaining = v,
                   c -> c.cooldownTicksRemaining
    ).add().build();

    public HealLeapAbilityComponent() {
        this.abilityEnabled = false;
        this.triggerHealthPercent = 0.25f;
        this.cooldownTicksRemaining = 0L;
        this.hitsTaken = 0;
        this.swapActive = false;
        this.swapSlot = -1;
        this.swapPreviousItem = null;
    }

    @Override
    public Component<EntityStore> clone() {
        HealLeapAbilityComponent c = new HealLeapAbilityComponent();
        c.abilityEnabled = this.abilityEnabled;
        c.triggerHealthPercent = this.triggerHealthPercent;
        c.cooldownTicksRemaining = this.cooldownTicksRemaining;
        return c;
    }
}
