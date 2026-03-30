package com.frotty27.rpgmobs.services;

import com.frotty27.rpgmobs.api.spawn.IRPGMobsSpawnAPI;
import com.frotty27.rpgmobs.api.spawn.SpawnResult;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.spawn.RPGMobsSpawnSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public final class RPGMobsSpawnAPI implements IRPGMobsSpawnAPI {

    private final RPGMobsPlugin plugin;

    public RPGMobsSpawnAPI(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpawnResult spawnElite(World world, String roleName, int tier,
                                  Vector3d position, @Nullable Vector3f rotation,
                                  @Nullable String weaponCategory) {
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) {
            return new SpawnResult.Failure(SpawnResult.Reason.CONFIG_NOT_LOADED,
                                          "RPGMobs config is not loaded yet.");
        }

        int tierIndex = Math.max(0, Math.min(4, tier - 1));

        EntityStore entityStoreProvider = world.getEntityStore();
        if (entityStoreProvider == null) {
            return new SpawnResult.Failure(SpawnResult.Reason.TIER_APPLY_FAILED,
                                          "World entity store is not available.");
        }
        Store<EntityStore> entityStore = entityStoreProvider.getStore();

        String resolvedRoleName = roleName;
        Pair<Ref<EntityStore>, ?> spawned;
        try {
            spawned = NPCPlugin.get().spawnNPC(entityStore, resolvedRoleName, null,
                                               position,
                                               rotation != null ? rotation : new Vector3f(0f, 0f, 0f));
        } catch (Exception e) {
            String fallback = resolvedRoleName + "_Fighter";
            int fallbackIdx = NPCPlugin.get().getIndex(fallback);
            if (fallbackIdx >= 0) {
                resolvedRoleName = fallback;
                try {
                    spawned = NPCPlugin.get().spawnNPC(entityStore, resolvedRoleName, null,
                                                       position,
                                                       rotation != null ? rotation : new Vector3f(0f, 0f, 0f));
                } catch (Exception e2) {
                    return new SpawnResult.Failure(SpawnResult.Reason.NPC_SPAWN_FAILED,
                                                  "Failed to spawn '" + resolvedRoleName + "': " + e2.getMessage());
                }
            } else {
                return new SpawnResult.Failure(SpawnResult.Reason.NPC_SPAWN_FAILED,
                                              "Failed to spawn '" + resolvedRoleName + "': " + e.getMessage()
                                              + ". Try using a spawnable variant (e.g. Skeleton_Fighter).");
            }
        }

        if (spawned == null || spawned.first() == null) {
            return new SpawnResult.Failure(SpawnResult.Reason.NPC_SPAWN_FAILED,
                                          "NPC spawn returned null for role: " + resolvedRoleName);
        }

        Ref<EntityStore> npcRef = spawned.first();
        return applyTierInternal(config, entityStore, npcRef, tierIndex, weaponCategory, resolvedRoleName);
    }

    @Override
    public SpawnResult applyEliteTier(World world, Ref<EntityStore> npcRef,
                                      int tier, @Nullable String weaponCategory) {
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) {
            return new SpawnResult.Failure(SpawnResult.Reason.CONFIG_NOT_LOADED,
                                          "RPGMobs config is not loaded yet.");
        }

        int tierIndex = Math.max(0, Math.min(4, tier - 1));

        EntityStore entityStoreProvider = world.getEntityStore();
        if (entityStoreProvider == null) {
            return new SpawnResult.Failure(SpawnResult.Reason.TIER_APPLY_FAILED,
                                          "World entity store is not available.");
        }
        Store<EntityStore> entityStore = entityStoreProvider.getStore();

        NPCEntity npcEntity = entityStore.getComponent(npcRef, NPC_COMPONENT_TYPE);
        if (npcEntity == null) {
            return new SpawnResult.Failure(SpawnResult.Reason.TIER_APPLY_FAILED,
                                          "Entity does not have an NPCEntity component.");
        }

        String roleName = npcEntity.getRoleName();
        return applyTierInternal(config, entityStore, npcRef, tierIndex, weaponCategory, roleName);
    }

    private SpawnResult applyTierInternal(RPGMobsConfig config, Store<EntityStore> entityStore,
                                          Ref<EntityStore> npcRef, int tierIndex,
                                          @Nullable String weaponCategory, String roleName) {
        RPGMobsSpawnSystem spawnSystem = plugin.getSpawnSystem();
        if (spawnSystem == null) {
            return new SpawnResult.Failure(SpawnResult.Reason.NOT_INITIALIZED,
                                          "RPGMobs spawn system is not available yet.");
        }

        NPCEntity npcEntity = entityStore.getComponent(npcRef, NPC_COMPONENT_TYPE);
        if (npcEntity == null) {
            return new SpawnResult.Failure(SpawnResult.Reason.TIER_APPLY_FAILED,
                                          "NPC entity data is not ready.");
        }

        AtomicReference<SpawnResult.@Nullable Reason> failureRef = new AtomicReference<>(null);
        AtomicReference<Boolean> applied = new AtomicReference<>(false);

        entityStore.forEachChunk((chunk, commandBuffer) -> {
            SpawnResult.@Nullable Reason reason = spawnSystem.applyTierForAPI(
                    config, npcRef, entityStore, commandBuffer, npcEntity, tierIndex, weaponCategory);
            failureRef.set(reason);
            applied.set(true);
            return false;
        });

        if (!applied.get()) {
            return new SpawnResult.Failure(SpawnResult.Reason.TIER_APPLY_FAILED,
                                          "Failed to obtain command buffer from entity store.");
        }

        SpawnResult.@Nullable Reason failure = failureRef.get();
        if (failure != null) {
            return new SpawnResult.Failure(failure, "Tier application failed: " + failure.name());
        }

        return new SpawnResult.Success(npcRef, tierIndex, roleName);
    }
}
