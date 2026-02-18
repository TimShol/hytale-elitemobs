package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.death.RPGMobsVanillaDropsCullSystem;
import com.frotty27.rpgmobs.systems.drops.RPGMobsExtraDropsSchedulerSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsDropsFeature implements IRPGMobsFeature {

    @Override
    public String getFeatureKey() {
        return "Drops";
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
        plugin.registerSystem(new RPGMobsVanillaDropsCullSystem(plugin));
        plugin.registerSystem(new RPGMobsExtraDropsSchedulerSystem(plugin));
    }
}
