package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsModelScalingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.visual.ModelScalingSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsModelScalingFeature implements IRPGMobsFeature {

    @Override
    public String getFeatureKey() {
        return "ModelScaling";
    }

    @Override
    public Object getConfig(RPGMobsConfig config) {
        return config.modelConfig;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        if (config.modelConfig.enableMobModelScaling) {
            RPGMobsModelScalingComponent modelScaling = new RPGMobsModelScalingComponent();
            commandBuffer.putComponent(npcRef, plugin.getModelScalingComponentType(), modelScaling);
        }
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
        ModelScalingSystem system = new ModelScalingSystem(plugin);
        plugin.registerSystem(system);
        plugin.getEventBus().registerListener(system);
    }
}
