package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.spawn.RPGMobsSpawnSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsSpawningFeature implements IRPGMobsFeature {

    private RPGMobsSpawnSystem spawnSystem;

    @Override
    public String getFeatureKey() {
        return "Spawning";
    }

    @Override
    public Object getConfig(RPGMobsConfig config) {
        return config.spawning;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
        spawnSystem = new RPGMobsSpawnSystem(plugin);
        plugin.registerSystem(spawnSystem);
    }

    public RPGMobsSpawnSystem getSpawnSystem() {
        return spawnSystem;
    }
}
