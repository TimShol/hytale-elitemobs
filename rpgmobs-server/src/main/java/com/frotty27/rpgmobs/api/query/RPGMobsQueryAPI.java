package com.frotty27.rpgmobs.api.query;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsHealthScalingComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsMigrationComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsModelScalingComponent;
import com.frotty27.rpgmobs.components.progression.RPGMobsProgressionComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;
import java.util.Set;

public class RPGMobsQueryAPI implements IRPGMobsQueryAPI {

    private final RPGMobsPlugin plugin;

    public RPGMobsQueryAPI(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    private Store<EntityStore> getStore(Ref<EntityStore> entityRef) {
        return entityRef.getStore();
    }


    @Override
    public Optional<Integer> getTier(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        return tier != null ? Optional.of(tier.tierIndex) : Optional.empty();
    }

    @Override
    public boolean isRPGMob(Ref<EntityStore> entityRef) {
        if (entityRef == null) return false;
        Store<EntityStore> store = getStore(entityRef);
        return store.getComponent(entityRef, plugin.getRPGMobsComponentType()) != null;
    }

    @Override
    public boolean isMinion(Ref<EntityStore> entityRef) {
        if (entityRef == null) return false;
        Store<EntityStore> store = getStore(entityRef);
        return store.getComponent(entityRef, plugin.getSummonedMinionComponentType()) != null;
    }

    @Override
    public Optional<Float> getDistanceHealthBonus(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsProgressionComponent prog = store.getComponent(entityRef, plugin.getProgressionComponentType());
        return prog != null ? Optional.of(prog.distanceHealthBonus()) : Optional.empty();
    }

    @Override
    public Optional<Float> getDistanceDamageBonus(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsProgressionComponent prog = store.getComponent(entityRef, plugin.getProgressionComponentType());
        return prog != null ? Optional.of(prog.distanceDamageBonus()) : Optional.empty();
    }

    @Override
    public Optional<Float> getSpawnDistance(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsProgressionComponent prog = store.getComponent(entityRef, plugin.getProgressionComponentType());
        return prog != null ? Optional.of(prog.spawnDistanceMeters()) : Optional.empty();
    }


    @Override
    public Optional<Float> getHealthMultiplier(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsHealthScalingComponent healthScaling = store.getComponent(entityRef,
                                                                         plugin.getHealthScalingComponentType()
        );
        return healthScaling != null && healthScaling.healthApplied ? Optional.of(healthScaling.appliedHealthMult) : Optional.empty();
    }

    @Override
    public Optional<Float> getDamageMultiplier(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();

        Store<EntityStore> store = getStore(entityRef);
        RPGMobsTierComponent tier = store.getComponent(entityRef, plugin.getRPGMobsComponentType());
        if (tier == null) return Optional.empty();

        float multiplier = 1.0f + (tier.tierIndex * 0.5f);


        RPGMobsProgressionComponent prog = store.getComponent(entityRef, plugin.getProgressionComponentType());
        if (prog != null) {
            multiplier += prog.distanceDamageBonus();
        }

        return Optional.of(multiplier);
    }

    @Override
    public Optional<Float> getModelScale(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsModelScalingComponent modelScaling = store.getComponent(entityRef,
                                                                       plugin.getModelScalingComponentType()
        );
        return modelScaling != null && modelScaling.scaledApplied ? Optional.of(modelScaling.appliedScale) : Optional.empty();
    }

    @Override
    public boolean isHealthFinalized(Ref<EntityStore> entityRef) {
        if (entityRef == null) return false;
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsHealthScalingComponent healthScaling = store.getComponent(entityRef,
                                                                         plugin.getHealthScalingComponentType()
        );
        return healthScaling != null && healthScaling.healthFinalized;
    }


    @Override
    public Optional<Integer> getSummonedMinionCount(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsSummonMinionTrackingComponent tracking = store.getComponent(entityRef,
                                                                           plugin.getSummonMinionTrackingComponentType()
        );
        return tracking != null ? Optional.of(tracking.summonedAliveCount) : Optional.empty();
    }


    @Override
    public Optional<Ref<EntityStore>> getLastAggroTarget(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsCombatTrackingComponent tracking = store.getComponent(entityRef,
                                                                     plugin.getCombatTrackingComponentType()
        );
        Ref<EntityStore> bestTarget = tracking != null ? tracking.getBestTarget() : null;
        return bestTarget != null ? Optional.of(bestTarget) : Optional.empty();
    }

    @Override
    public Optional<Long> getLastAggroTick(Ref<EntityStore> entityRef) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsCombatTrackingComponent tracking = store.getComponent(entityRef,
                                                                     plugin.getCombatTrackingComponentType()
        );
        return tracking != null && tracking.stateChangedTick > 0 ? Optional.of(tracking.stateChangedTick) : Optional.empty();
    }

    @Override
    public boolean isInCombat(Ref<EntityStore> entityRef) {
        if (entityRef == null) return false;
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsCombatTrackingComponent tracking = store.getComponent(entityRef,
                                                                     plugin.getCombatTrackingComponentType()
        );
        if (tracking == null) return false;

        return tracking.isInCombat();
    }


    @Override
    public int getMigrationVersion(Ref<EntityStore> entityRef) {
        if (entityRef == null) return 1;
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsMigrationComponent migration = store.getComponent(entityRef, plugin.getMigrationComponentType());
        return migration != null ? migration.migrationVersion : 1;
    }

    @Override
    public boolean needsMigration(Ref<EntityStore> entityRef) {
        if (entityRef == null) return false;
        Store<EntityStore> store = getStore(entityRef);
        RPGMobsMigrationComponent migration = store.getComponent(entityRef, plugin.getMigrationComponentType());
        return migration != null && migration.needsMigration();
    }


    @Override
    public Set<String> getSupportedTriggerTypes() {
        return Set.of("aggro", "damage_received", "ability_completed", "death", "deaggro");
    }

    @Override
    public boolean isTriggerTypeSupported(String triggerType) {
        if (triggerType == null) return false;
        return getSupportedTriggerTypes().contains(triggerType.toLowerCase());
    }
}
