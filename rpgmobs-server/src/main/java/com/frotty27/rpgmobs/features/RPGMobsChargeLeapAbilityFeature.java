package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
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

public final class RPGMobsChargeLeapAbilityFeature implements IRPGMobsAbilityFeature {

    public static final String ABILITY_CHARGE_LEAP = AbilityIds.CHARGE_LEAP;

    private final Random random = new Random();

    @Override
    public String id() {
        return ABILITY_CHARGE_LEAP;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        RPGMobsConfig.ChargeLeapAbilityConfig abilityConfig = (RPGMobsConfig.ChargeLeapAbilityConfig) config.abilitiesConfig.defaultAbilities.get(
                AbilityIds.CHARGE_LEAP);

        if (abilityConfig == null) return;

        int tierIndex = tierComponent.tierIndex;

        if (!AbilityGateEvaluator.isAllowed(abilityConfig, roleName, "", tierIndex)) return;

        float spawnChance = tierIndex < abilityConfig.chancePerTier.length ? abilityConfig.chancePerTier[tierIndex] : 0f;

        boolean enabled = random.nextFloat() < spawnChance;

        ChargeLeapAbilityComponent component = new ChargeLeapAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;

        commandBuffer.putComponent(npcRef, plugin.getChargeLeapAbilityComponentType(), component);
    }
}
