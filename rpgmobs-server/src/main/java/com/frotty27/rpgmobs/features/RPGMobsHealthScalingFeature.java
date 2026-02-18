package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsHealthScalingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.visual.HealthScalingSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsHealthScalingFeature implements IRPGMobsFeature {

    @Override
    public String getFeatureKey() {
        return "HealthScaling";
    }

    @Override
    public Object getConfig(RPGMobsConfig config) {
        return config.healthConfig;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        if (config.healthConfig.enableMobHealthScaling) {
            RPGMobsHealthScalingComponent healthScaling = new RPGMobsHealthScalingComponent();
            commandBuffer.putComponent(npcRef, plugin.getHealthScalingComponentType(), healthScaling);
        }
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
        HealthScalingSystem system = new HealthScalingSystem(plugin, this);
        plugin.registerSystem(system);
        plugin.getEventBus().registerListener(system);
    }
}
