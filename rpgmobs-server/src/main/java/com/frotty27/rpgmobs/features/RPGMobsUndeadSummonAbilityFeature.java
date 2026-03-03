package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.SummonUndeadAbilityComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonMinionTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
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

        RPGMobsConfig.AbilityConfig rawSummonConfig = config.abilitiesConfig.defaultAbilities.get(AbilityIds.SUMMON_UNDEAD);
        RPGMobsConfig.SummonAbilityConfig abilityConfig = (rawSummonConfig instanceof RPGMobsConfig.SummonAbilityConfig sm) ? sm : null;

        if (abilityConfig == null) {
            return;
        }

        int tierIndex = tierComponent.tierIndex;
        ResolvedConfig resolved = RPGMobsAbilityFeatureHelpers.resolveConfig(plugin, npcRef, entityStore);
        String weaponId = RPGMobsAbilityFeatureHelpers.resolveWeaponId(npcRef, entityStore);
        String matchedRuleKey = tierComponent.matchedRuleKey;

        if (!AbilityGateEvaluator.isAllowed(abilityConfig, AbilityIds.SUMMON_UNDEAD, weaponId, tierIndex, matchedRuleKey, resolved)) {
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
    public void reconcile(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                          Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                          RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        SummonUndeadAbilityComponent comp = entityStore.getComponent(npcRef, plugin.getSummonUndeadAbilityComponentType());

        RPGMobsConfig.AbilityConfig rawSummonRecon = config.abilitiesConfig.defaultAbilities.get(AbilityIds.SUMMON_UNDEAD);
        RPGMobsConfig.SummonAbilityConfig abilityConfig = (rawSummonRecon instanceof RPGMobsConfig.SummonAbilityConfig sm) ? sm : null;

        if (abilityConfig == null) {
            if (comp != null && comp.abilityEnabled) {
                comp.abilityEnabled = false;
                comp.pendingSummonTicksRemaining = 0L;
                comp.pendingSummonRole = null;
                commandBuffer.replaceComponent(npcRef, plugin.getSummonUndeadAbilityComponentType(), comp);
            }
            return;
        }

        int tierIndex = tierComponent.tierIndex;
        ResolvedConfig resolved = RPGMobsAbilityFeatureHelpers.resolveConfig(plugin, npcRef, entityStore);
        String weaponId = RPGMobsAbilityFeatureHelpers.resolveWeaponId(npcRef, entityStore);
        String matchedRuleKey = tierComponent.matchedRuleKey;

        boolean allowed = AbilityGateEvaluator.isAllowed(abilityConfig, AbilityIds.SUMMON_UNDEAD, weaponId, tierIndex, matchedRuleKey, resolved);

        if (comp == null) {
            if (allowed) {
                apply(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
            }
            return;
        }

        if (!allowed && comp.abilityEnabled) {
            comp.abilityEnabled = false;
            comp.pendingSummonTicksRemaining = 0L;
            comp.pendingSummonRole = null;
            commandBuffer.replaceComponent(npcRef, plugin.getSummonUndeadAbilityComponentType(), comp);
        } else if (allowed && !comp.abilityEnabled) {
            apply(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
        }
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
    }
}
