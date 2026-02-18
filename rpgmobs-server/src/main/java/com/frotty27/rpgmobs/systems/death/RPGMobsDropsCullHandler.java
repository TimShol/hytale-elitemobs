package com.frotty27.rpgmobs.systems.death;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

final class RPGMobsDropsCullHandler {

    private final RPGMobsVanillaDropsCullSystem system;

    RPGMobsDropsCullHandler(RPGMobsVanillaDropsCullSystem system) {
        this.system = system;
    }

    void handle(int entityIndex, @NonNull ArchetypeChunk<EntityStore> archetypeChunk,
                @NonNull CommandBuffer<EntityStore> commandBuffer) {
        system.processTick(entityIndex, archetypeChunk, commandBuffer);
    }
}
