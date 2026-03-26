package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.RPGMobsConfig.AbilityConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.frotty27.rpgmobs.systems.ability.AbilityTriggerSource;
import com.frotty27.rpgmobs.systems.ability.TriggerContext;
import com.frotty27.rpgmobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
    public String description() {
        return "Summons undead minions into battle";
    }

    @Override
    public Set<AbilityTriggerSource> triggerSources() {
        return Set.of(AbilityTriggerSource.AGGRO, AbilityTriggerSource.ABILITY_COMPLETED);
    }

    @Override
    public boolean canTrigger(TriggerContext context) {
        var summon = RPGMobsAbilityFeatureHelpers.getReadyAbilityComponent(
                context, context.plugin().getSummonUndeadAbilityComponentType());
        if (summon == null) return false;

        RPGMobsConfig.AbilityConfig rawConfig = context.config().abilitiesConfig.defaultAbilities.get(id());
        if (!(rawConfig instanceof RPGMobsConfig.SummonAbilityConfig abilityConfig)) return false;

        RPGMobsSummonMinionTrackingComponent tracking = context.store().getComponent(
                context.entityRef(), context.plugin().getSummonMinionTrackingComponentType()
        );
        if (tracking != null) {
            int maxAlive = Math.max(0, Math.min(50, abilityConfig.maxAliveMinionsPerSummoner));
            if (!tracking.canSummonMore(maxAlive)) return false;
        }

        return true;
    }

    @Override
    public String resolveRootTemplateKey(TriggerContext context) {
        int variation = ThreadLocalRandom.current().nextInt(2);
        return switch (variation) {
            case 0 -> AbilityConfig.TEMPLATE_ROOT_INTERACTION;
            case 1 -> "rootV2";
            default -> super.resolveRootTemplateKey(context);
        };
    }

    @Override
    public void onPreChainStart(TriggerContext context, NPCEntity npcEntity) {
        SummonUndeadAbilityComponent summon = context.store().getComponent(
                context.entityRef(), context.plugin().getSummonUndeadAbilityComponentType()
        );
        if (summon == null) return;
        AbilityHelpers.unequipWeaponInHand(npcEntity, summon);
    }

    @Override
    public void onChainStartFailed(TriggerContext context, NPCEntity npcEntity) {
        SummonUndeadAbilityComponent summon = context.store().getComponent(
                context.entityRef(), context.plugin().getSummonUndeadAbilityComponentType()
        );
        if (summon != null) {
            AbilityHelpers.restoreWeaponIfNeeded(npcEntity, summon);
            summon.pendingSummonRole = null;
            summon.pendingSummonTicksRemaining = 0L;
        }
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
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).excludeFromSummonPool = value),
                new AbilityConfigField.ScalarInt("Summon Min Count",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).summonMinCount,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).summonMinCount = value),
                new AbilityConfigField.ScalarInt("Summon Max Count",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).summonMaxCount,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).summonMaxCount = value),
                new AbilityConfigField.ScalarDouble("Summon Spawn Radius",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).summonSpawnRadius,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).summonSpawnRadius = value),
                new AbilityConfigField.ScalarInt("Minion Min Tier",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).minionMinTier,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).minionMinTier = value),
                new AbilityConfigField.ScalarInt("Minion Max Tier",
                        config -> ((RPGMobsConfig.SummonAbilityConfig) config).minionMaxTier,
                        (config, value) -> ((RPGMobsConfig.SummonAbilityConfig) config).minionMaxTier = value)
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
