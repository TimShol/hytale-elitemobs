package com.frotty27.elitemobs.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.exceptions.FeatureRegistrationException;
import com.frotty27.elitemobs.exceptions.FeatureResolutionException;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

public final class EliteMobsFeatureRegistry {

    private static EliteMobsFeatureRegistry instance;

    private final List<IEliteMobsFeature> orderedFeatures = new ArrayList<>();
    private final Map<String, IEliteMobsFeature> featuresByKey = new HashMap<>();
    private final Map<String, IEliteMobsFeature> featuresByAssetId = new HashMap<>();

    public EliteMobsFeatureRegistry(EliteMobsPlugin plugin) {
        instance = this;
        register(new EliteMobsSpawningFeature());
        register(new EliteMobsDamageFeature());
        register(new EliteMobsAbilityCoreFeature());
        register(new EliteMobsDropsFeature());
        register(new EliteMobsNameplateFeature());
        register(new EliteMobsProjectileResistanceEffectFeature(plugin));
        register(new EliteMobsChargeLeapAbilityFeature());
        register(new EliteMobsHealLeapAbilityFeature());
        register(new EliteMobsUndeadSummonAbilityFeature());
        register(new EliteMobsConsumablesFeature());
        register(new EliteMobsHealthScalingFeature());
        register(new EliteMobsModelScalingFeature());
    }

    public static EliteMobsFeatureRegistry getInstance() {
        return instance;
    }

    public void register(IEliteMobsFeature feature) {
        if (feature == null) return;
        
        if (featuresByKey.containsKey(feature.getFeatureKey())) {
            throw new FeatureRegistrationException("Duplicate feature key: " + feature.getFeatureKey());
        }
        
        orderedFeatures.add(feature);
        featuresByKey.put(feature.getFeatureKey(), feature);
        
        String assetId = feature.getAssetId();
        if (assetId != null) {
            if (featuresByAssetId.containsKey(assetId)) {
                throw new FeatureRegistrationException("Duplicate feature asset ID: " + assetId);
            }
            featuresByAssetId.put(assetId, feature);
        }
    }
    
    public IEliteMobsFeature getFeature(String key) {
        IEliteMobsFeature feature = featuresByKey.get(key);
        if (feature == null) {
            throw new FeatureResolutionException("Key: " + key);
        }
        return feature;
    }
    
    public IEliteMobsFeature getFeatureByAssetId(String assetId) {
        IEliteMobsFeature feature = featuresByAssetId.get(assetId);
        if (feature == null) {
            throw new FeatureResolutionException("AssetID: " + assetId);
        }
        return feature;
    }

    public Map<String, IEliteMobsFeature> getFeaturesByKey() {
        return featuresByKey;
    }

    public void applyAll(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
        for (IEliteMobsFeature feature : orderedFeatures) {
            feature.apply(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
        }
    }

    public void reconcileAll(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
        for (IEliteMobsFeature feature : orderedFeatures) {
            feature.reconcile(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
        }
    }

    public void registerSystems(EliteMobsPlugin plugin) {
        for (IEliteMobsFeature feature : orderedFeatures) {
            feature.registerSystems(plugin);
        }
    }

    public void onDamageAll(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> victimRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable NPCEntity npcEntity,
            int tierIndex,
            long currentTick,
            Damage damage
    ) {
        for (IEliteMobsFeature feature : orderedFeatures) {
            feature.onDamage(plugin, config, victimRef, entityStore, commandBuffer, tierComponent, npcEntity, tierIndex, currentTick, damage);
        }
    }
}
