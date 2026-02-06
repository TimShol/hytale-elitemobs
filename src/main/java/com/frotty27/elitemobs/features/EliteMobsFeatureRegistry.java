package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.ability.EliteMobsHealAbilityFeature;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class EliteMobsFeatureRegistry {

    private final List<EliteMobsFeature> features = new ArrayList<>();

    public EliteMobsFeatureRegistry(EliteMobsPlugin plugin) {
        register(new NameplateFeature());
        register(new ProjectileResistanceFeature(plugin));
        register(new LeapAbilityFeature());
        register(new EliteMobsHealAbilityFeature());
        register(new HealthScalingFeature());
        register(new ModelScalingFeature());
    }

    public void register(EliteMobsFeature feature) {
        if (feature == null) return;
        features.add(feature);
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
        for (EliteMobsFeature feature : features) {
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
        for (EliteMobsFeature feature : features) {
            feature.reconcile(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
        }
    }

    public void registerSystems(EliteMobsPlugin plugin) {
        for (EliteMobsFeature feature : features) {
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
            com.hypixel.hytale.server.core.modules.entity.damage.Damage damage
    ) {
        for (EliteMobsFeature feature : features) {
            feature.onDamage(plugin, config, victimRef, entityStore, commandBuffer, tierComponent, npcEntity, tierIndex, currentTick, damage);
        }
    }
}
