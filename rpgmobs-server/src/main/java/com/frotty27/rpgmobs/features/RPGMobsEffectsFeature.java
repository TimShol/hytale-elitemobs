package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.assets.AssetConfigHelpers;
import com.frotty27.rpgmobs.assets.AssetType;
import com.frotty27.rpgmobs.assets.RPGMobsAssetRetriever;
import com.frotty27.rpgmobs.assets.TieredAssetConfig;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.data.EffectState;
import com.frotty27.rpgmobs.components.effects.RPGMobsActiveEffectsComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;

public final class RPGMobsEffectsFeature implements IRPGMobsFeature {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final RPGMobsPlugin plugin;
    private RPGMobsAssetRetriever assetRetriever;

    public RPGMobsEffectsFeature(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    private RPGMobsAssetRetriever getAssetLoader() {
        if (assetRetriever == null && plugin != null) {
            assetRetriever = plugin.getRPGMobsAssetLoader();
        }
        return assetRetriever;
    }

    @Override
    public String getFeatureKey() {
        return "Effects";
    }

    @Override
    public Object getConfig(RPGMobsConfig config) {
        return config.effectsConfig;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        var activeEffects = new RPGMobsActiveEffectsComponent();
        commandBuffer.putComponent(npcRef, plugin.getActiveEffectsComponentType(), activeEffects);

        applyAllEffects(config, npcRef, entityStore, commandBuffer, tierComponent, activeEffects);
    }

    @Override
    public void reconcile(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                          Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                          RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        var activeEffects = entityStore.getComponent(npcRef, plugin.getActiveEffectsComponentType());
        if (activeEffects == null) {
            activeEffects = new RPGMobsActiveEffectsComponent();
            commandBuffer.putComponent(npcRef, plugin.getActiveEffectsComponentType(), activeEffects);
        }

        reconcileAllEffects(config, npcRef, entityStore, commandBuffer, tierComponent, activeEffects);
    }

    private void applyAllEffects(RPGMobsConfig config, Ref<EntityStore> npcRef,
                                 Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                                 RPGMobsTierComponent tierComponent,
                                 RPGMobsActiveEffectsComponent activeEffects) {
        int tierIndex = clampTierIndex(tierComponent.tierIndex);
        boolean debug = config.debugConfig.isDebugModeEnabled;

        var effectController = entityStore.getComponent(npcRef, EffectControllerComponent.getComponentType());
        if (effectController == null) {
            if (debug) {
                RPGMobsLogger.debug(LOGGER, "[Effects Apply All] EffectControllerComponent is null — skipping", RPGMobsLogLevel.WARNING);
            }
            return;
        }

        Map<String, RPGMobsConfig.EntityEffectConfig> allEffects = config.effectsConfig.defaultEntityEffects;
        if (allEffects == null || allEffects.isEmpty()) {
            if (debug) {
                RPGMobsLogger.debug(LOGGER, "[Effects Apply All] No configured effects — skipping", RPGMobsLogLevel.INFO);
            }
            return;
        }

        if (debug) {
            RPGMobsLogger.debug(LOGGER, "[Effects Apply All] tier=T%d effects=%d", RPGMobsLogLevel.INFO, tierIndex + 1, allEffects.size());
        }

        boolean modified = false;

        for (Map.Entry<String, RPGMobsConfig.EntityEffectConfig> entry : allEffects.entrySet()) {
            String effectKey = entry.getKey();

            var effectConfig = (TieredAssetConfig) AssetConfigHelpers.getAssetConfig(config, AssetType.EFFECTS, effectKey);
            boolean enabled = AssetConfigHelpers.isTieredAssetConfigEnabledForTier(effectConfig, tierIndex);

            if (debug) {
                boolean isEnabled = effectConfig != null && effectConfig.isEnabled;
                boolean[] perTier = effectConfig != null ? effectConfig.isEnabledPerTier : null;
                RPGMobsLogger.debug(LOGGER,
                                    "[Effects Apply All] effectKey='%s' tier=T%d enabled=%b (isEnabled=%b, perTier=%s)",
                                    RPGMobsLogLevel.INFO, effectKey, tierIndex + 1, enabled, isEnabled,
                                    perTier != null ? Arrays.toString(perTier) : "null");
            }

            if (enabled) {
                modified |= applyEffect(config, npcRef, entityStore, effectController, activeEffects, effectKey, effectConfig, tierIndex);
            }
        }

        if (modified) {
            commandBuffer.replaceComponent(npcRef, EffectControllerComponent.getComponentType(), effectController);
            commandBuffer.replaceComponent(npcRef, plugin.getActiveEffectsComponentType(), activeEffects);
        }
    }

    private void reconcileAllEffects(RPGMobsConfig config, Ref<EntityStore> npcRef,
                                     Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                                     RPGMobsTierComponent tierComponent,
                                     RPGMobsActiveEffectsComponent activeEffects) {
        int tierIndex = clampTierIndex(tierComponent.tierIndex);
        boolean debug = config.debugConfig.isDebugModeEnabled;

        var effectController = entityStore.getComponent(npcRef, EffectControllerComponent.getComponentType());
        if (effectController == null) {
            if (debug) {
                RPGMobsLogger.debug(LOGGER, "[Effects Reconcile] EffectControllerComponent is null — skipping", RPGMobsLogLevel.WARNING);
            }
            return;
        }

        Map<String, RPGMobsConfig.EntityEffectConfig> allEffects = config.effectsConfig.defaultEntityEffects;
        if (debug) {
            int effectCount = allEffects != null ? allEffects.size() : 0;
            int activeCount = activeEffects.activeEffects.size();
            RPGMobsLogger.debug(LOGGER, "[Effects Reconcile] tier=T%d configuredEffects=%d activeEffects=%d activeKeys=%s",
                                RPGMobsLogLevel.INFO, tierIndex + 1, effectCount, activeCount,
                                activeEffects.activeEffects.keySet());
        }
        if (allEffects == null || allEffects.isEmpty()) {
            if (!activeEffects.activeEffects.isEmpty()) {
                removeAllEffects(config, npcRef, entityStore, commandBuffer, effectController, activeEffects, tierIndex);
            }
            return;
        }

        Set<String> desiredEffects = new HashSet<>();
        boolean modified = false;

        for (Map.Entry<String, RPGMobsConfig.EntityEffectConfig> entry : allEffects.entrySet()) {
            String effectKey = entry.getKey();

            var effectConfig = (TieredAssetConfig) AssetConfigHelpers.getAssetConfig(config, AssetType.EFFECTS, effectKey);
            boolean enabled = AssetConfigHelpers.isTieredAssetConfigEnabledForTier(effectConfig, tierIndex);

            if (debug) {
                boolean isEnabled = effectConfig != null && effectConfig.isEnabled;
                boolean[] perTier = effectConfig != null ? effectConfig.isEnabledPerTier : null;
                RPGMobsLogger.debug(LOGGER,
                                    "[Effects Reconcile] effectKey='%s' tier=T%d enabled=%b (isEnabled=%b, perTier=%s)",
                                    RPGMobsLogLevel.INFO, effectKey, tierIndex + 1, enabled, isEnabled,
                                    perTier != null ? Arrays.toString(perTier) : "null");
            }

            if (enabled) {
                desiredEffects.add(effectKey);
                modified |= applyEffect(config, npcRef, entityStore, effectController, activeEffects, effectKey, effectConfig, tierIndex);
            }
        }

        Set<String> toRemove = new HashSet<>();
        for (String activeKey : activeEffects.activeEffects.keySet()) {
            if (!desiredEffects.contains(activeKey)) {
                toRemove.add(activeKey);
            }
        }

        if (debug && !toRemove.isEmpty()) {
            RPGMobsLogger.debug(LOGGER, "[Effects Reconcile] Removing %d stale effects: %s",
                                RPGMobsLogLevel.INFO, toRemove.size(), toRemove);
        }

        for (String removeKey : toRemove) {
            modified |= removeEffect(config, npcRef, entityStore, effectController, activeEffects, removeKey, tierIndex);
        }

        if (modified) {
            commandBuffer.replaceComponent(npcRef, EffectControllerComponent.getComponentType(), effectController);
            commandBuffer.replaceComponent(npcRef, plugin.getActiveEffectsComponentType(), activeEffects);
        }
    }

    private boolean applyEffect(RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                EffectControllerComponent effectController,
                                RPGMobsActiveEffectsComponent activeEffects,
                                String effectKey, TieredAssetConfig effectConfig, int tierIndex) {
        boolean debug = config.debugConfig.isDebugModeEnabled;

        String effectId = AssetConfigHelpers.getTieredAssetIdFromOnlyTemplate(config, effectConfig, tierIndex);
        if (effectId == null || effectId.isBlank()) {
            if (debug) {
                RPGMobsLogger.debug(LOGGER, "[Effects Apply] effectKey='%s' tier=T%d — effectId is null/blank (asset not generated?)",
                                    RPGMobsLogLevel.WARNING, effectKey, tierIndex + 1);
            }
            return false;
        }

        int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);

        if (activeEffects.activeEffects.containsKey(effectKey)) {
            if (hasActiveEffectIndex(effectController, effectIndex)) {
                if (debug) {
                    RPGMobsLogger.debug(LOGGER, "[Effects Apply] effectKey='%s' tier=T%d — already active (index=%d), skipping",
                                        RPGMobsLogLevel.INFO, effectKey, tierIndex + 1, effectIndex);
                }
                return false;
            }
            if (debug) {
                RPGMobsLogger.debug(LOGGER, "[Effects Apply] effectKey='%s' tier=T%d — tracked but not on controller, re-applying",
                                    RPGMobsLogLevel.INFO, effectKey, tierIndex + 1);
            }
            activeEffects.removeEffect(effectKey);
        }

        var loader = getAssetLoader();
        EntityEffect effectAsset = loader != null ? loader.getAsset(effectId, EntityEffect.getAssetMap()::getAsset) : null;
        if (effectAsset == null) {
            if (debug) {
                RPGMobsLogger.debug(LOGGER, "[Effects Apply] effectKey='%s' tier=T%d id='%s' — EntityEffect asset not found (loader=%s)",
                                    RPGMobsLogLevel.WARNING, effectKey, tierIndex + 1, effectId,
                                    loader != null ? "present" : "NULL");
            }
            return false;
        }

        boolean applied = effectController.addInfiniteEffect(npcRef, effectIndex, effectAsset, entityStore);
        if (!applied) {
            if (debug) {
                RPGMobsLogger.debug(LOGGER, "[Effects Apply] effectKey='%s' tier=T%d id='%s' index=%d — addInfiniteEffect() returned FALSE",
                                    RPGMobsLogLevel.WARNING, effectKey, tierIndex + 1, effectId, effectIndex);
            }
            return false;
        }

        activeEffects.addEffect(effectKey, new EffectState(0L, -1L, 1, true));

        if (debug) {
            RPGMobsLogger.debug(LOGGER, "[Effects Apply] SUCCESS effectKey='%s' tier=T%d id='%s' index=%d",
                                RPGMobsLogLevel.INFO, effectKey, tierIndex + 1, effectId, effectIndex);
        }

        return true;
    }

    private boolean removeEffect(RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                 EffectControllerComponent effectController,
                                 RPGMobsActiveEffectsComponent activeEffects,
                                 String effectKey, int tierIndex) {
        boolean debug = config.debugConfig.isDebugModeEnabled;
        var effectConfig = (TieredAssetConfig) AssetConfigHelpers.getAssetConfig(config, AssetType.EFFECTS, effectKey);

        String effectId = AssetConfigHelpers.getTieredAssetIdFromOnlyTemplate(config, effectConfig, tierIndex);
        if (effectId == null || effectId.isBlank()) {
            if (debug) {
                RPGMobsLogger.debug(LOGGER, "[Effects Remove] effectKey='%s' — effectId null/blank, removing from tracking only",
                                    RPGMobsLogLevel.INFO, effectKey);
            }
            activeEffects.removeEffect(effectKey);
            return true;
        }

        int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);

        if (hasActiveEffectIndex(effectController, effectIndex)) {
            effectController.removeEffect(npcRef, effectIndex, entityStore);
            if (debug) {
                RPGMobsLogger.debug(LOGGER, "[Effects Remove] SUCCESS effectKey='%s' tier=T%d id='%s' index=%d — removed from controller",
                                    RPGMobsLogLevel.INFO, effectKey, tierIndex + 1, effectId, effectIndex);
            }
        } else if (debug) {
            RPGMobsLogger.debug(LOGGER, "[Effects Remove] effectKey='%s' tier=T%d — not found on controller (index=%d), removing tracking only",
                                RPGMobsLogLevel.INFO, effectKey, tierIndex + 1, effectIndex);
        }

        activeEffects.removeEffect(effectKey);

        return true;
    }

    public void clearAllEffects(RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                               CommandBuffer<EntityStore> commandBuffer, int tierIndex) {
        var activeEffects = entityStore.getComponent(npcRef, plugin.getActiveEffectsComponentType());
        if (activeEffects == null || activeEffects.activeEffects.isEmpty()) return;
        var effectController = entityStore.getComponent(npcRef, EffectControllerComponent.getComponentType());
        if (effectController == null) return;
        removeAllEffects(config, npcRef, entityStore, commandBuffer, effectController, activeEffects, tierIndex);
    }

    private void removeAllEffects(RPGMobsConfig config, Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                  CommandBuffer<EntityStore> commandBuffer,
                                  EffectControllerComponent effectController,
                                  RPGMobsActiveEffectsComponent activeEffects, int tierIndex) {
        boolean modified = false;
        for (String activeKey : new HashSet<>(activeEffects.activeEffects.keySet())) {
            modified |= removeEffect(config, npcRef, entityStore, effectController, activeEffects, activeKey, tierIndex);
        }

        if (modified) {
            commandBuffer.replaceComponent(npcRef, EffectControllerComponent.getComponentType(), effectController);
            commandBuffer.replaceComponent(npcRef, plugin.getActiveEffectsComponentType(), activeEffects);
        }
    }

    private static boolean hasActiveEffectIndex(EffectControllerComponent effectController, int effectIndex) {
        var activeEffects = effectController.getAllActiveEntityEffects();
        if (activeEffects == null) return false;

        for (var activeEffect : activeEffects) {
            if (activeEffect != null && activeEffect.getEntityEffectIndex() == effectIndex) return true;
        }

        return false;
    }
}
