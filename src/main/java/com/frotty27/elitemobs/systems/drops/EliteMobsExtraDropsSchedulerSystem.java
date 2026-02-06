package com.frotty27.elitemobs.systems.drops;

import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public final class EliteMobsExtraDropsSchedulerSystem extends EntityTickingSystem<EntityStore> {

    private final EliteMobsPlugin eliteMobsPlugin;
    private final EliteMobsExtraDropsTickHandler tickHandler = new EliteMobsExtraDropsTickHandler(this);

    // Ensures we only advance/flush once per world tick, not once per entity.
    private int lastSeenCommandBufferIdentityHash = Integer.MIN_VALUE;

    public EliteMobsExtraDropsSchedulerSystem(EliteMobsPlugin eliteMobsPlugin) {
        this.eliteMobsPlugin = eliteMobsPlugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Exists on basically everything, so this system runs in active worlds.
        return TransformComponent.getComponentType();
    }

    @Override
    public void tick(
            float deltaTimeSeconds,
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        tickHandler.handle(deltaTimeSeconds, archetypeChunk, entityStore, commandBuffer);
    }

    void processTick(
            float deltaTimeSeconds,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        int commandBufferIdentityHash = System.identityHashCode(commandBuffer);
        if (commandBufferIdentityHash == lastSeenCommandBufferIdentityHash) return;
        lastSeenCommandBufferIdentityHash = commandBufferIdentityHash;

        eliteMobsPlugin.onWorldTick();
        eliteMobsPlugin.getTickClock().advance(deltaTimeSeconds);
        eliteMobsPlugin.getExtraDropsScheduler().flushDue(entityStore, commandBuffer);
    }

}
