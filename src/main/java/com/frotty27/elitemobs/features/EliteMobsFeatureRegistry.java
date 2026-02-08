package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.exception.FeatureRegistrationException;
import com.frotty27.elitemobs.exception.FeatureResolutionException;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EliteMobsFeatureRegistry {

    private static EliteMobsFeatureRegistry instance;

    private final List<EliteMobsFeature> orderedFeatures = new ArrayList<>();
    private final Map<String, EliteMobsFeature> featuresByKey = new HashMap<>();
    private final Map<String, EliteMobsFeature> featuresByAssetId = new HashMap<>();

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

    public void register(EliteMobsFeature feature) {
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
    
    public EliteMobsFeature getFeature(String key) {
        EliteMobsFeature feature = featuresByKey.get(key);
        if (feature == null) {
            throw new FeatureResolutionException("Key: " + key);
        }
        return feature;
    }
    
    public EliteMobsFeature getFeatureByAssetId(String assetId) {
        EliteMobsFeature feature = featuresByAssetId.get(assetId);
        if (feature == null) {
            throw new FeatureResolutionException("AssetID: " + assetId);
        }
        return feature;
    }

    public Map<String, EliteMobsFeature> getFeaturesByKey() {
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
        for (EliteMobsFeature feature : orderedFeatures) {
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
        for (EliteMobsFeature feature : orderedFeatures) {
            feature.reconcile(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
        }
    }

    public void registerSystems(EliteMobsPlugin plugin) {
        for (EliteMobsFeature feature : orderedFeatures) {
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
        for (EliteMobsFeature feature : orderedFeatures) {
            feature.onDamage(plugin, config, victimRef, entityStore, commandBuffer, tierComponent, npcEntity, tierIndex, currentTick, damage);
        }
    }
}
