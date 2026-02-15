package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.spawn.EliteMobsSpawnSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class EliteMobsSpawningFeature implements IEliteMobsFeature {

    private EliteMobsSpawnSystem spawnSystem;

    @Override
    public String getFeatureKey() {
        return "Spawning";
    }

    @Override
    public Object getConfig(EliteMobsConfig config) {
        return config.spawning;
    }

    @Override
    public void apply(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        spawnSystem = new EliteMobsSpawnSystem(plugin);
        plugin.registerSystem(spawnSystem);
    }

    public EliteMobsSpawnSystem getSpawnSystem() {
        return spawnSystem;
    }
}
