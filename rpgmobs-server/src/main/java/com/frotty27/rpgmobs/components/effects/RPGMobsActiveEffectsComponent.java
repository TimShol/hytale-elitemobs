package com.frotty27.rpgmobs.components.effects;

import com.frotty27.rpgmobs.components.data.EffectState;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.Map;

public final class RPGMobsActiveEffectsComponent implements Component<EntityStore> {

    public Map<String, EffectState> activeEffects;

    public static final BuilderCodec<RPGMobsActiveEffectsComponent> CODEC = BuilderCodec.builder(
            RPGMobsActiveEffectsComponent.class,
            RPGMobsActiveEffectsComponent::new
    ).build();

    public RPGMobsActiveEffectsComponent() {
        this.activeEffects = new HashMap<>();
    }

    @Override
    public Component<EntityStore> clone() {
        RPGMobsActiveEffectsComponent c = new RPGMobsActiveEffectsComponent();
        c.activeEffects = new HashMap<>(this.activeEffects);
        return c;
    }

    public void addEffect(String effectId, EffectState state) {
        activeEffects.put(effectId, state);
    }

    public void removeEffect(String effectId) {
        activeEffects.remove(effectId);
    }
}
