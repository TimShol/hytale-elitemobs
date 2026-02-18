package com.frotty27.rpgmobs.systems.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

final class RPGMobsDamageScalingHandler {

    private final RPGMobsDamageDealtSystem system;

    RPGMobsDamageScalingHandler(RPGMobsDamageDealtSystem system) {
        this.system = system;
    }

    void handle(int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                @NonNull Store<EntityStore> entityStore, @NonNull CommandBuffer<EntityStore> commandBuffer,
                @NonNull Damage damage) {
        system.processHandle(entityIndex, archetypeChunk, entityStore, commandBuffer, damage);
    }
}
