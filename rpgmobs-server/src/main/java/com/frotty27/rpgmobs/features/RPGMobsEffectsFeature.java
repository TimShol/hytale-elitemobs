package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.effects.RPGMobsActiveEffectsComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsEffectsFeature implements IRPGMobsFeature {

    @Override
    public String getFeatureKey() {
        return "Effects";
    }

    @Override
    public Object getConfig(RPGMobsConfig config) {
        return config.effectsConfig;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        RPGMobsActiveEffectsComponent effects = new RPGMobsActiveEffectsComponent();
        commandBuffer.putComponent(npcRef, plugin.getActiveEffectsComponentType(), effects);
    }
}
