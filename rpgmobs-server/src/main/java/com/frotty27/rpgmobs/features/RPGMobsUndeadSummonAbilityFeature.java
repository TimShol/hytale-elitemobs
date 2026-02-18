package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.rules.AbilityGateEvaluator;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.Random;

public final class RPGMobsUndeadSummonAbilityFeature implements IRPGMobsAbilityFeature {

    public static final String ABILITY_UNDEAD_SUMMON = AbilityIds.SUMMON_UNDEAD;

    private final Random random = new Random();

    @Override
    public String id() {
        return ABILITY_UNDEAD_SUMMON;
    }


    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {

        RPGMobsConfig.SummonAbilityConfig abilityConfig = (RPGMobsConfig.SummonAbilityConfig) config.abilitiesConfig.defaultAbilities.get(
                AbilityIds.SUMMON_UNDEAD);

        if (abilityConfig == null) {
            return;
        }

        int tierIndex = tierComponent.tierIndex;

        if (!AbilityGateEvaluator.isAllowed(abilityConfig, roleName, "", tierIndex)) {
            return;
        }


        float spawnChance = tierIndex < abilityConfig.chancePerTier.length ? abilityConfig.chancePerTier[tierIndex] : 0f;

        boolean enabled = random.nextFloat() < spawnChance;

        SummonUndeadAbilityComponent component = new SummonUndeadAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        component.pendingSummonTicksRemaining = 0L;
        component.pendingSummonRole = null;

        commandBuffer.putComponent(npcRef, plugin.getSummonUndeadAbilityComponentType(), component);

        RPGMobsSummonMinionTrackingComponent trackingComponent = new RPGMobsSummonMinionTrackingComponent();
        commandBuffer.putComponent(npcRef, plugin.getSummonMinionTrackingComponentType(), trackingComponent);

    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
    }
}
