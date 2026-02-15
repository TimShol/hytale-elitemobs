package com.frotty27.elitemobs.api.query;

import java.util.*;

import com.frotty27.elitemobs.components.*;
import com.frotty27.elitemobs.components.combat.*;
import com.frotty27.elitemobs.components.effects.*;
import com.frotty27.elitemobs.components.lifecycle.*;
import com.frotty27.elitemobs.components.progression.*;
import com.frotty27.elitemobs.components.summon.*;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class EliteMobsQueryAPI implements IEliteMobsQueryAPI {

    private final EliteMobsPlugin plugin;

    public EliteMobsQueryAPI(EliteMobsPlugin plugin) {
        this.plugin = plugin;
    }


    private Store<EntityStore> getStore(Ref<EntityStore> entityRef) {
        return entityRef.getStore();
    }


    @Override
    public Optional<Integer> getTier(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsTierComponent tier = store.getComponent(entityRef, plugin.getEliteMobsComponent());
        return tier != null ? Optional.of(tier.tierIndex) : Optional.empty();
    }

    @Override
    public boolean isEliteMob(Ref<EntityStore> entityRef) {
        if (entityRef == null) return false;
        Store<EntityStore> store = getStore(entityRef);
        return store.getComponent(entityRef, plugin.getEliteMobsComponent()) != null;
    }


    @Override
    public Optional<Float> getDistanceHealthBonus(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsProgressionComponent prog = store.getComponent(entityRef, plugin.getProgressionComponent());
        return prog != null ? Optional.of(prog.distanceHealthBonus()) : Optional.empty();
    }

    @Override
    public Optional<Float> getDistanceDamageBonus(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsProgressionComponent prog = store.getComponent(entityRef, plugin.getProgressionComponent());
        return prog != null ? Optional.of(prog.distanceDamageBonus()) : Optional.empty();
    }

    @Override
    public Optional<Float> getSpawnDistance(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsProgressionComponent prog = store.getComponent(entityRef, plugin.getProgressionComponent());
        return prog != null ? Optional.of(prog.spawnDistanceMeters()) : Optional.empty();
    }


    @Override
    public Optional<Float> getHealthMultiplier(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsHealthScalingComponent healthScaling = store.getComponent(entityRef, plugin.getHealthScalingComponent());
        return healthScaling != null && healthScaling.healthApplied ? Optional.of(healthScaling.appliedHealthMult) : Optional.empty();
    }

    @Override
    public Optional<Float> getDamageMultiplier(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();

        float multiplier = 1.0f;

        Store<EntityStore> store = getStore(entityRef);
        EliteMobsTierComponent tier = store.getComponent(entityRef, plugin.getEliteMobsComponent());
        if (tier == null) return Optional.empty();

        multiplier = 1.0f + (tier.tierIndex * 0.5f);


        EliteMobsProgressionComponent prog = store.getComponent(entityRef, plugin.getProgressionComponent());
        if (prog != null) {
            multiplier += prog.distanceDamageBonus();
        }

        return Optional.of(multiplier);
    }

    @Override
    public Optional<Float> getModelScale(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsModelScalingComponent modelScaling = store.getComponent(entityRef, plugin.getModelScalingComponent());
        return modelScaling != null && modelScaling.scaledApplied ? Optional.of(modelScaling.appliedScale) : Optional.empty();
    }

    @Override
    public boolean isHealthFinalized(Ref<EntityStore> entityRef) {
        if (entityRef == null) return false;
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsHealthScalingComponent healthScaling = store.getComponent(entityRef, plugin.getHealthScalingComponent());
        return healthScaling != null && healthScaling.healthFinalized;
    }


    @Override
    public Optional<Integer> getSummonedMinionCount(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsSummonMinionTrackingComponent tracking = store.getComponent(entityRef, plugin.getSummonMinionTrackingComponent());
        return tracking != null ? Optional.of(tracking.summonedAliveCount) : Optional.empty();
    }


    @Override
    public Optional<Ref<EntityStore>> getLastAggroTarget(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsCombatTrackingComponent tracking = store.getComponent(entityRef, plugin.getCombatTrackingComponent());
        Ref<EntityStore> bestTarget = tracking != null ? tracking.getBestTarget() : null;
        return bestTarget != null ? Optional.of(bestTarget) : Optional.empty();
    }

    @Override
    public Optional<Long> getLastAggroTick(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsCombatTrackingComponent tracking = store.getComponent(entityRef, plugin.getCombatTrackingComponent());
        return tracking != null && tracking.stateChangedTick > 0 ? Optional.of(tracking.stateChangedTick) : Optional.empty();
    }

    @Override
    public boolean isInCombat(Ref<EntityStore> entityRef) {
        if (entityRef == null) return false;
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsCombatTrackingComponent tracking = store.getComponent(entityRef, plugin.getCombatTrackingComponent());
        if (tracking == null) return false;

        return tracking.isInCombat();
    }


    @Override
    public int getMigrationVersion(Ref<EntityStore> entityRef) {
        if (entityRef == null) return 1;
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsMigrationComponent migration = store.getComponent(entityRef, plugin.getMigrationComponent());
        return migration != null ? migration.migrationVersion : 1;
    }

    @Override
    public boolean needsMigration(Ref<EntityStore> entityRef) {
        if (entityRef == null) return false;
        Store<EntityStore> store = getStore(entityRef);
        EliteMobsMigrationComponent migration = store.getComponent(entityRef, plugin.getMigrationComponent());
        return migration != null && migration.needsMigration();
    }


    @Override
    public Set<String> getSupportedTriggerTypes() {
        return Set.of(
            "aggro",
            "damage_received",
            "ability_completed",
            "death",
            "deaggro"
        );
    }

    @Override
    public boolean isTriggerTypeSupported(String triggerType) {
        if (triggerType == null) return false;
        return getSupportedTriggerTypes().contains(triggerType.toLowerCase());
    }
}
