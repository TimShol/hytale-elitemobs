package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

public interface IRPGMobsFeature {

    @Nullable
    default String getAssetId() {
        return null;
    }

    String getFeatureKey();

    @Nullable
    default Object getConfig(RPGMobsConfig config) {
        return null;
    }

    void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
               CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent, @Nullable String roleName);

    default void reconcile(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                           Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                           RPGMobsTierComponent tierComponent, @Nullable String roleName) {
    }

    default void registerSystems(RPGMobsPlugin plugin) {
    }

    default void onDamage(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> victimRef,
                          Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                          RPGMobsTierComponent tierComponent, @Nullable NPCEntity npcEntity, int tierIndex,
                          long currentTick, com.hypixel.hytale.server.core.modules.entity.damage.Damage damage) {
    }
}
