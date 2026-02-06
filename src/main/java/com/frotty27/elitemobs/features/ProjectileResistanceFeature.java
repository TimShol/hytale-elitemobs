package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.assets.AssetConfigHelpers;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.assets.EliteMobsAssetCatalog;
import com.frotty27.elitemobs.assets.EliteMobsAssetLoader;
import com.frotty27.elitemobs.assets.TieredAssetConfig;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.log.EliteMobsLogLevel;
import com.frotty27.elitemobs.log.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;

public final class ProjectileResistanceFeature implements EliteMobsFeature {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final EliteMobsPlugin plugin;
    private final EliteMobsAssetLoader eliteMobsAssetLoader;

    public ProjectileResistanceFeature(EliteMobsPlugin plugin) {
        this.plugin = plugin;
        this.eliteMobsAssetLoader = plugin.getEliteMobsAssetLoader();
    }

    @Override
    public String id() {
        return "effect_projectile_resistance";
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
        reconcile(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
    }

    @Override
    public void reconcile(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
        int tierIndex = clampTierIndex(tierComponent.tierIndex);

        TieredAssetConfig effectConfig = (TieredAssetConfig) AssetConfigHelpers.getAssetConfig(
                config,
                AssetType.EFFECTS,
                EliteMobsAssetCatalog.EFFECT_PROJECTILE_RESISTANCE
        );

        boolean enabled = AssetConfigHelpers.isTieredAssetConfigEnabledForTier(effectConfig, tierIndex);

        EffectControllerComponent effectController =
                entityStore.getComponent(npcRef, EffectControllerComponent.getComponentType());
        if (effectController == null) return;

        if (!enabled) {
            if (!tierComponent.projectileResistApplied) return;
            removeEffectIfPresent(config, npcRef, entityStore, commandBuffer, effectController, tierComponent, tierIndex);
            return;
        }

        String effectId = AssetConfigHelpers.getTieredAssetIdFromOnlyTemplate(config, effectConfig, tierIndex);
        if (effectId == null || effectId.isBlank()) return;

        int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);

        if (tierComponent.projectileResistApplied) {
            if (hasActiveEffectIndex(effectController, effectIndex)) return;
            tierComponent.projectileResistApplied = false;
        }

        EntityEffect effectAsset =
                eliteMobsAssetLoader.getAsset(effectId, EntityEffect.getAssetMap()::getAsset);
        if (effectAsset == null) return;

        boolean applied = effectController.addInfiniteEffect(npcRef, effectIndex, effectAsset, entityStore);
        if (!applied) return;

        commandBuffer.replaceComponent(npcRef, EffectControllerComponent.getComponentType(), effectController);

        tierComponent.projectileResistApplied = true;
        commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);

        if (config.debug.isDebugModeEnabled) {
            EliteMobsLogger.debug(
                    LOGGER,
                    "Applied projectile resistance tier=%d id=%s",
                    EliteMobsLogLevel.INFO,
                    tierIndex,
                    effectId
            );
        }
    }

    private void removeEffectIfPresent(
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EffectControllerComponent effectController,
            EliteMobsTierComponent tierComponent,
            int tierIndex
    ) {
        TieredAssetConfig effectConfig = (TieredAssetConfig) AssetConfigHelpers.getAssetConfig(
                config,
                AssetType.EFFECTS,
                EliteMobsAssetCatalog.EFFECT_PROJECTILE_RESISTANCE
        );

        String effectId = AssetConfigHelpers.getTieredAssetIdFromOnlyTemplate(config, effectConfig, tierIndex);
        if (effectId == null || effectId.isBlank()) return;

        int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);

        if (hasActiveEffectIndex(effectController, effectIndex)) {
            effectController.removeEffect(npcRef, effectIndex, entityStore);
            commandBuffer.replaceComponent(npcRef, EffectControllerComponent.getComponentType(), effectController);
        }

        tierComponent.projectileResistApplied = false;
        commandBuffer.replaceComponent(npcRef, plugin.getEliteMobsComponent(), tierComponent);
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
