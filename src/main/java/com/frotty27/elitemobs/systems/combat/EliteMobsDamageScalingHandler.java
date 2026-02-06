package com.frotty27.elitemobs.systems.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

final class EliteMobsDamageScalingHandler {

    private final EliteMobsDamageDealtSystem system;

    EliteMobsDamageScalingHandler(EliteMobsDamageDealtSystem system) {
        this.system = system;
    }

    void handle(
            int entityIndex,
            @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
            @NonNull Store<EntityStore> entityStore,
            @NonNull CommandBuffer<EntityStore> commandBuffer,
            @NonNull Damage damage
    ) {
        system.processHandle(entityIndex, archetypeChunk, entityStore, commandBuffer, damage);
    }
}
