package com.frotty27.rpgmobs.services;

import com.frotty27.rpgmobs.api.query.IRPGMobsQueryAPI;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.progression.RPGMobsProgressionComponent;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;

public class RPGMobsQueryAPI implements IRPGMobsQueryAPI {

    private final RPGMobsPlugin plugin;

    public RPGMobsQueryAPI(RPGMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<Integer> getTier(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getRPGMobsComponentType())
                .map(tier -> tier.tierIndex);
    }

    @Override
    public boolean isRPGMob(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getRPGMobsComponentType()).isPresent();
    }

    @Override
    public boolean isMinion(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getSummonedMinionComponentType()).isPresent();
    }

    @Override
    public Optional<Float> getDistanceHealthBonus(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getProgressionComponentType())
                .map(RPGMobsProgressionComponent::distanceHealthBonus);
    }

    @Override
    public Optional<Float> getDistanceDamageBonus(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getProgressionComponentType())
                .map(RPGMobsProgressionComponent::distanceDamageBonus);
    }

    @Override
    public Optional<Float> getSpawnDistance(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getProgressionComponentType())
                .map(RPGMobsProgressionComponent::spawnDistanceMeters);
    }

    @Override
    public Optional<Float> getHealthMultiplier(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getHealthScalingComponentType())
                .filter(healthScaling -> healthScaling.healthApplied)
                .map(healthScaling -> healthScaling.appliedHealthMult);
    }

    @Override
    public Optional<Float> getDamageMultiplier(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getRPGMobsComponentType())
                .map(tier -> {
                    float multiplier = 1.0f + (tier.tierIndex * 0.5f);
                    var prog = queryComponent(entityRef, plugin.getProgressionComponentType());
                    if (prog.isPresent()) {
                        multiplier += prog.get().distanceDamageBonus();
                    }
                    return multiplier;
                });
    }

    @Override
    public Optional<Float> getModelScale(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getModelScalingComponentType())
                .filter(modelScaling -> modelScaling.scaleApplied)
                .map(modelScaling -> modelScaling.appliedScale);
    }

    @Override
    public boolean isHealthFinalized(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getHealthScalingComponentType())
                .map(healthScaling -> healthScaling.healthFinalized)
                .orElse(false);
    }

    @Override
    public Optional<Integer> getSummonedMinionCount(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getSummonMinionTrackingComponentType())
                .map(tracking -> tracking.summonedAliveCount);
    }

    @Override
    public Optional<Ref<EntityStore>> getLastAggroTarget(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getCombatTrackingComponentType())
                .map(RPGMobsCombatTrackingComponent::getBestTarget)
                .filter(target -> target != null);
    }

    @Override
    public Optional<Long> getLastAggroTick(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getCombatTrackingComponentType())
                .filter(tracking -> tracking.stateChangedTick > 0)
                .map(tracking -> tracking.stateChangedTick);
    }

    @Override
    public boolean isInCombat(Ref<EntityStore> entityRef) {
        return queryComponent(entityRef, plugin.getCombatTrackingComponentType())
                .map(RPGMobsCombatTrackingComponent::isInCombat)
                .orElse(false);
    }

    private <C extends Component<EntityStore>> Optional<C> queryComponent(Ref<EntityStore> entityRef,
                                            ComponentType<EntityStore, C> componentType) {
        if (entityRef == null) return Optional.empty();
        Store<EntityStore> store = entityRef.getStore();
        return Optional.ofNullable(store.getComponent(entityRef, componentType));
    }
}
