package com.frotty27.elitemobs.systems.drops;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

final class EliteMobsExtraDropsTickHandler {

    private final EliteMobsExtraDropsSchedulerSystem system;

    EliteMobsExtraDropsTickHandler(EliteMobsExtraDropsSchedulerSystem system) {
        this.system = system;
    }

    void handle(
            float deltaTimeSeconds,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer
    ) {
        system.processTick(deltaTimeSeconds, archetypeChunk, entityStore, commandBuffer);
    }
}
