package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Random;

public final class RPGMobsUndeadSummonAbilityFeature
        extends AbstractGatedAbilityFeature<SummonUndeadAbilityComponent, RPGMobsConfig.SummonAbilityConfig> {

    @Override
    public String id() {
        return AbilityIds.SUMMON_UNDEAD;
    }

    @Override
    public String displayName() {
        return "Undead Summon";
    }

    @Override
    public RPGMobsConfig.AbilityConfig createDefaultConfig() {
        return new RPGMobsConfig.SummonAbilityConfig();
    }

    @Override
    public List<AbilityConfigField> describeConfigFields() {
        return List.of(
                new AbilityConfigField.ScalarInt("Max Alive Minions",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).maxAliveMinionsPerSummoner,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).maxAliveMinionsPerSummoner = value),
                new AbilityConfigField.ScalarDouble("Skeleton Archer Weight",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).skeletonArcherWeight,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).skeletonArcherWeight = value),
                new AbilityConfigField.ScalarDouble("Zombie Weight",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).zombieWeight,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).zombieWeight = value),
                new AbilityConfigField.ScalarDouble("Wraith Weight",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).wraithWeight,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).wraithWeight = value),
                new AbilityConfigField.ScalarDouble("Aberrant Weight",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).aberrantWeight,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).aberrantWeight = value),
                new AbilityConfigField.StringList("Role Identifiers",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).roleIdentifiers,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).roleIdentifiers = value),
                new AbilityConfigField.StringList("Exclude From Summon Pool",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).excludeFromSummonPool,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).excludeFromSummonPool = value)
        );
    }

    @Override
    protected Class<RPGMobsConfig.SummonAbilityConfig> configClass() {
        return RPGMobsConfig.SummonAbilityConfig.class;
    }

    @Override
    protected ComponentType<EntityStore, SummonUndeadAbilityComponent> componentType(RPGMobsPlugin plugin) {
        return plugin.getSummonUndeadAbilityComponentType();
    }

    @Override
    protected SummonUndeadAbilityComponent getComponent(Store<EntityStore> store, Ref<EntityStore> ref,
                                                        RPGMobsPlugin plugin) {
        return store.getComponent(ref, plugin.getSummonUndeadAbilityComponentType());
    }

    @Override
    protected SummonUndeadAbilityComponent createComponent(RPGMobsConfig.SummonAbilityConfig abilityConfig,
                                                           int tierIndex, boolean enabled, Random random) {
        var component = new SummonUndeadAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        component.pendingSummonTicksRemaining = 0L;
        component.pendingSummonRole = null;
        return component;
    }

    @Override
    protected void onDisable(SummonUndeadAbilityComponent component) {
        component.pendingSummonTicksRemaining = 0L;
        component.pendingSummonRole = null;
    }

    @Override
    protected void afterApply(RPGMobsPlugin plugin, Ref<EntityStore> npcRef,
                              Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        var trackingComponent = new RPGMobsSummonMinionTrackingComponent();
        commandBuffer.putComponent(npcRef, plugin.getSummonMinionTrackingComponentType(), trackingComponent);
    }

    @Override
    protected void afterRemove(RPGMobsPlugin plugin, Ref<EntityStore> npcRef,
                               Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        commandBuffer.tryRemoveComponent(npcRef, plugin.getSummonMinionTrackingComponentType());
    }
}
