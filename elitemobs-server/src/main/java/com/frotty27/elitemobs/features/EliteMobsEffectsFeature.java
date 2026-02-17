package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.effects.EliteMobsActiveEffectsComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class EliteMobsEffectsFeature implements IEliteMobsFeature {

    @Override
    public String getFeatureKey() {
        return "Effects";
    }

    @Override
    public Object getConfig(EliteMobsConfig config) {
        return config.effectsConfig;
    }

    @Override
    public void apply(EliteMobsPlugin plugin, EliteMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      EliteMobsTierComponent tierComponent, @Nullable String roleName) {
        EliteMobsActiveEffectsComponent effects = new EliteMobsActiveEffectsComponent();
        commandBuffer.putComponent(npcRef, plugin.getActiveEffectsComponentType(), effects);
    }
}
