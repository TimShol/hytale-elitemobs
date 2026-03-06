package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Random;

public final class RPGMobsHealLeapAbilityFeature
        extends AbstractGatedAbilityFeature<HealLeapAbilityComponent, RPGMobsConfig.HealLeapAbilityConfig> {

    @Override
    public String id() {
        return AbilityIds.HEAL_LEAP;
    }

    @Override
    public String displayName() {
        return "Heal Leap";
    }

    @Override
    public RPGMobsConfig.AbilityConfig createDefaultConfig() {
        return new RPGMobsConfig.HealLeapAbilityConfig();
    }

    @Override
    public List<AbilityConfigField> describeConfigFields() {
        return List.of(
                new AbilityConfigField.ScalarFloat("Min Health Trigger %",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).minHealthTriggerPercent,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).minHealthTriggerPercent = value),
                new AbilityConfigField.ScalarFloat("Max Health Trigger %",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).maxHealthTriggerPercent,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).maxHealthTriggerPercent = value),
                new AbilityConfigField.ScalarFloat("Instant Heal Chance",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).instantHealChance,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).instantHealChance = value),
                new AbilityConfigField.ScalarFloat("Drink Duration (s)",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).npcDrinkDurationSeconds,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).npcDrinkDurationSeconds = value),
                new AbilityConfigField.ScalarString("Drink Item ID",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).npcDrinkItemId,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).npcDrinkItemId = value),
                new AbilityConfigField.PerTierFloat("Instant Heal Amount",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).instantHealAmountPerTier,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).instantHealAmountPerTier = value),
                new AbilityConfigField.PerTierFloat("Apply Force",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).applyForcePerTier,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).applyForcePerTier = value)
        );
    }

    @Override
    protected Class<RPGMobsConfig.HealLeapAbilityConfig> configClass() {
        return RPGMobsConfig.HealLeapAbilityConfig.class;
    }

    @Override
    protected ComponentType<EntityStore, HealLeapAbilityComponent> componentType(RPGMobsPlugin plugin) {
        return plugin.getHealLeapAbilityComponentType();
    }

    @Override
    protected HealLeapAbilityComponent getComponent(Store<EntityStore> store, Ref<EntityStore> ref,
                                                    RPGMobsPlugin plugin) {
        return store.getComponent(ref, plugin.getHealLeapAbilityComponentType());
    }

    @Override
    protected HealLeapAbilityComponent createComponent(RPGMobsConfig.HealLeapAbilityConfig abilityConfig,
                                                       int tierIndex, boolean enabled, Random random) {
        float triggerHealthPercent = abilityConfig.minHealthTriggerPercent
                + random.nextFloat() * (abilityConfig.maxHealthTriggerPercent - abilityConfig.minHealthTriggerPercent);

        var component = new HealLeapAbilityComponent();
        component.abilityEnabled = enabled;
        component.triggerHealthPercent = triggerHealthPercent;
        component.cooldownTicksRemaining = 0L;
        return component;
    }
}
