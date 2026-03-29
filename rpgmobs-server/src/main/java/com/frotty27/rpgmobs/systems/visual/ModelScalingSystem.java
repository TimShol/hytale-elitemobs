package com.frotty27.rpgmobs.systems.visual;

import com.frotty27.rpgmobs.api.IRPGMobsEventListener;
import com.frotty27.rpgmobs.api.events.RPGMobsSpawnedEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsModelScalingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.utils.StoreHelpers;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.Random;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampFloat;
import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public class ModelScalingSystem extends EntityTickingSystem<EntityStore> implements IRPGMobsEventListener {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float MODEL_SCALE_MIN = 0.5f;
    private static final float MODEL_SCALE_MAX = 2.0f;
    private final Random random = new Random();
    private final RPGMobsPlugin plugin;

    public ModelScalingSystem(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPC_COMPONENT_TYPE, plugin.getRPGMobsComponentType());
    }

    @Override
    public void tick(float deltaTime, int entityIndex, @NonNull ArchetypeChunk<EntityStore> chunk,
                     @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;

        Ref<EntityStore> npcRef = chunk.getReferenceTo(entityIndex);
        RPGMobsTierComponent tierComp = store.getComponent(npcRef, plugin.getRPGMobsComponentType());
        if (tierComp == null) return;

        NPCEntity npcEntity = store.getComponent(npcRef, NPC_COMPONENT_TYPE);
        String worldName = (npcEntity != null && npcEntity.getWorld() != null) ? npcEntity.getWorld().getName() : null;
        ResolvedConfig resolved = plugin.getResolvedConfig(worldName);

        RPGMobsModelScalingComponent modelComp = store.getComponent(npcRef, plugin.getModelScalingComponentType());

        // Apply initial scaling before reconcile gate (mirrors HealthScalingSystem pattern)
        if (modelComp != null && !modelComp.scaleApplied) {
            if (resolved.enableModelScaling) {
                float targetBaseScale = getBaseScaleMultiplier(resolved, tierComp.tierIndex);
                float scaleWithVariance = applyVariance(targetBaseScale, resolved.modelScaleVariance);
                boolean scaled = tryScaleModelComponent(npcRef, store, commandBuffer, scaleWithVariance, false);
                if (scaled) {
                    modelComp.scaleApplied = true;
                    modelComp.appliedScale = scaleWithVariance;
                    modelComp.configuredBaseScale = targetBaseScale;
                    modelComp.resyncVerified = true;
                    commandBuffer.replaceComponent(npcRef, plugin.getModelScalingComponentType(), modelComp);
                }
            }
            return;
        }

        boolean needsReconcile = plugin.shouldReconcileThisTick()
                || tierComp.lastReconciledAt < plugin.getConfigReloadCount();
        if (!needsReconcile) return;

        if (!resolved.enableModelScaling) {

            if (modelComp != null && modelComp.scaleApplied && Math.abs(modelComp.appliedScale - 1.0f) > 0.001f) {
                tryScaleModelComponent(npcRef, store, commandBuffer, 1.0f, false);
                modelComp.scaleApplied = false;
                modelComp.appliedScale = 1.0f;
                modelComp.configuredBaseScale = 1.0f;
                modelComp.resyncVerified = true;
                commandBuffer.replaceComponent(npcRef, plugin.getModelScalingComponentType(), modelComp);
            }
            return;
        }

        float targetBaseScale = getBaseScaleMultiplier(resolved, tierComp.tierIndex);

        if (modelComp == null) {

            float scaleWithVariance = applyVariance(targetBaseScale, resolved.modelScaleVariance);
            boolean scaled = tryScaleModelComponent(npcRef, store, commandBuffer, scaleWithVariance, false);
            if (scaled) {
                RPGMobsModelScalingComponent newComp = new RPGMobsModelScalingComponent();
                newComp.scaleApplied = true;
                newComp.appliedScale = scaleWithVariance;
                newComp.configuredBaseScale = targetBaseScale;
                newComp.resyncVerified = true;
                commandBuffer.putComponent(npcRef, plugin.getModelScalingComponentType(), newComp);
            }
            return;
        }

        boolean configChanged = Math.abs(modelComp.configuredBaseScale - targetBaseScale) > 0.001f;
        if (configChanged) {
            float modelVariance = resolved.modelScaleVariance;
            float minValid = targetBaseScale - modelVariance;
            float maxValid = targetBaseScale + modelVariance;
            float currentScale = modelComp.appliedScale;

            if (currentScale >= minValid - 0.001f && currentScale <= maxValid + 0.001f) {
                modelComp.configuredBaseScale = targetBaseScale;
                modelComp.resyncVerified = true;
                commandBuffer.replaceComponent(npcRef, plugin.getModelScalingComponentType(), modelComp);
            } else {
                float newScale = currentScale < minValid
                        ? clampFloat(minValid, MODEL_SCALE_MIN, MODEL_SCALE_MAX)
                        : clampFloat(maxValid, MODEL_SCALE_MIN, MODEL_SCALE_MAX);
                boolean scaled = tryScaleModelComponent(npcRef, store, commandBuffer, newScale, false);
                if (scaled) {
                    modelComp.scaleApplied = true;
                    modelComp.appliedScale = newScale;
                    modelComp.configuredBaseScale = targetBaseScale;
                    modelComp.resyncVerified = true;
                    commandBuffer.replaceComponent(npcRef, plugin.getModelScalingComponentType(), modelComp);
                }
            }
            return;
        }

        if (!modelComp.resyncVerified && modelComp.scaleApplied && modelComp.appliedScale > 0.001f) {
            tryScaleModelComponent(npcRef, store, commandBuffer, modelComp.appliedScale, false);
            modelComp.resyncVerified = true;
            commandBuffer.replaceComponent(npcRef, plugin.getModelScalingComponentType(), modelComp);
        }
    }

    private static float getBaseScaleMultiplier(ResolvedConfig resolved, int tierIndex) {
        if (resolved.modelScalePerTier != null && tierIndex >= 0 && tierIndex < resolved.modelScalePerTier.length) {
            return resolved.modelScalePerTier[tierIndex];
        }
        return 1.0f;
    }

    private float applyVariance(float baseScale, float variance) {
        float v = Math.max(0f, variance);
        float randomized = baseScale + ((random.nextFloat() * 2f - 1f) * v);
        return clampFloat(randomized, MODEL_SCALE_MIN, MODEL_SCALE_MAX);
    }

    public void resetModelScale(Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                               CommandBuffer<EntityStore> commandBuffer) {
        tryScaleModelComponent(npcRef, entityStore, commandBuffer, 1.0f, false);
    }

    private boolean tryScaleModelComponent(Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                           CommandBuffer<EntityStore> commandBuffer, float scaleMultiplier,
                                           boolean log) {
        ModelComponent modelComponent = entityStore.getComponent(npcRef, ModelComponent.getComponentType());
        if (modelComponent == null) return false;

        Model currentModel = modelComponent.getModel();
        if (currentModel == null) return false;

        String modelAssetId = currentModel.getModelAssetId();
        if (modelAssetId == null || modelAssetId.isBlank()) return false;

        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(modelAssetId);
        if (modelAsset == null) {
            if (log) {
                RPGMobsLogger.debug(LOGGER,
                                    "ModelAsset not found id=%s (cannot scale)",
                                    RPGMobsLogLevel.WARNING,
                                    modelAssetId
                );
            }
            return false;
        }

        Model scaledModel;
        try {
            scaledModel = Model.createScaledModel(modelAsset,
                                                  scaleMultiplier,
                                                  currentModel.getRandomAttachmentIds(),
                                                  currentModel.getBoundingBox()
            );
        } catch (Throwable ignored) {
            scaledModel = Model.createStaticScaledModel(modelAsset, scaleMultiplier);
        }

        commandBuffer.replaceComponent(npcRef, ModelComponent.getComponentType(), new ModelComponent(scaledModel));

        PersistentModel persistentModel = entityStore.getComponent(npcRef, PersistentModel.getComponentType());
        if (persistentModel != null) {
            persistentModel.setModelReference(scaledModel.toReference());
            commandBuffer.replaceComponent(npcRef, PersistentModel.getComponentType(), persistentModel);
        }

        if (log) {
            RPGMobsLogger.debug(LOGGER,
                                "Scaled model asset=%s scale=%.3f",
                                RPGMobsLogLevel.INFO,
                                modelAssetId,
                                scaleMultiplier
            );
        }

        return true;
    }

    public void applyModelScalingOnSpawn(Ref<EntityStore> npcRef, Store<EntityStore> store,
                                         CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsConfig config = plugin.getConfig();
        if (config == null) return;

        NPCEntity npcEntityForSpawn = store.getComponent(npcRef, NPC_COMPONENT_TYPE);
        String spawnWorldName = (npcEntityForSpawn != null && npcEntityForSpawn.getWorld() != null)
                ? npcEntityForSpawn.getWorld().getName() : null;
        ResolvedConfig resolved = plugin.getResolvedConfig(spawnWorldName);

        if (!resolved.enableModelScaling) return;

        RPGMobsTierComponent tierComponent = store.getComponent(npcRef, plugin.getRPGMobsComponentType());
        RPGMobsModelScalingComponent modelScalingComponent = store.getComponent(npcRef,
                                                                                plugin.getModelScalingComponentType()
        );

        if (tierComponent == null) return;

        if (modelScalingComponent != null && modelScalingComponent.scaleApplied) return;

        int tierIndex = tierComponent.tierIndex;
        float baseScale = getBaseScaleMultiplier(resolved, tierIndex);
        float scaleMultiplier = applyVariance(baseScale, resolved.modelScaleVariance);

        boolean scaled = tryScaleModelComponent(npcRef,
                                                store,
                                                commandBuffer,
                                                scaleMultiplier,
                                                config.debugConfig.isDebugModeEnabled
        );

        if (!scaled) return;

        if (modelScalingComponent != null) {
            modelScalingComponent.scaleApplied = true;
            modelScalingComponent.appliedScale = scaleMultiplier;
            modelScalingComponent.configuredBaseScale = baseScale;
            commandBuffer.replaceComponent(npcRef, plugin.getModelScalingComponentType(), modelScalingComponent);
        }
    }

    @Override
    public void onRPGMobSpawned(RPGMobsSpawnedEvent event) {
        if (event.isCancelled()) return;

        Ref<EntityStore> npcRef = event.getEntityRef();
        Store<EntityStore> store = npcRef.getStore();

        NPCEntity npcEntity = store.getComponent(npcRef, NPC_COMPONENT_TYPE);
        if (npcEntity == null || npcEntity.getWorld() == null) return;

        var world = npcEntity.getWorld();
        world.execute(() -> {
            if (npcEntity.getWorld() == null) return;
            EntityStore entityStoreProvider = npcEntity.getWorld().getEntityStore();
            if (entityStoreProvider == null) return;
            if (!npcRef.isValid()) return;
            Store<EntityStore> entityStore = entityStoreProvider.getStore();

            StoreHelpers.withEntity(entityStore,
                                    npcRef,
                                    (_, commandBuffer, _) -> applyModelScalingOnSpawn(npcRef,
                                                                                      entityStore,
                                                                                      commandBuffer
                                    )
            );
        });
    }
}
