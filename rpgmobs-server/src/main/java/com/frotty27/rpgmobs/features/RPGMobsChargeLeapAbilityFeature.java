package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Random;

public final class RPGMobsChargeLeapAbilityFeature
        extends AbstractGatedAbilityFeature<ChargeLeapAbilityComponent, RPGMobsConfig.ChargeLeapAbilityConfig> {

    @Override
    public String id() {
        return AbilityIds.CHARGE_LEAP;
    }

    @Override
    public String displayName() {
        return "Charge Leap";
    }

    @Override
    public RPGMobsConfig.AbilityConfig createDefaultConfig() {
        return new RPGMobsConfig.ChargeLeapAbilityConfig();
    }

    @Override
    public List<AbilityConfigField> describeConfigFields() {
        return List.of(
                new AbilityConfigField.ScalarFloat("Min Range",
                        config -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).minRange,
                        (config, value) -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).minRange = value),
                new AbilityConfigField.ScalarFloat("Max Range",
                        config -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).maxRange,
                        (config, value) -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).maxRange = value),
                new AbilityConfigField.ScalarBoolean("Face Target",
                        config -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).faceTarget,
                        (config, value) -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).faceTarget = value),
                new AbilityConfigField.PerTierFloat("Slam Range",
                        config -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).slamRangePerTier,
                        (config, value) -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).slamRangePerTier = value),
                new AbilityConfigField.PerTierInt("Slam Base Damage",
                        config -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).slamBaseDamagePerTier,
                        (config, value) -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).slamBaseDamagePerTier = value),
                new AbilityConfigField.PerTierFloat("Apply Force",
                        config -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).applyForcePerTier,
                        (config, value) -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).applyForcePerTier = value),
                new AbilityConfigField.PerTierFloat("Knockback Lift",
                        config -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).knockbackLiftPerTier,
                        (config, value) -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).knockbackLiftPerTier = value),
                new AbilityConfigField.PerTierFloat("Knockback Push",
                        config -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).knockbackPushAwayPerTier,
                        (config, value) -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).knockbackPushAwayPerTier = value),
                new AbilityConfigField.PerTierFloat("Knockback Force",
                        config -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).knockbackForcePerTier,
                        (config, value) -> ((RPGMobsConfig.ChargeLeapAbilityConfig) config).knockbackForcePerTier = value)
        );
    }

    @Override
    protected Class<RPGMobsConfig.ChargeLeapAbilityConfig> configClass() {
        return RPGMobsConfig.ChargeLeapAbilityConfig.class;
    }

    @Override
    protected ComponentType<EntityStore, ChargeLeapAbilityComponent> componentType(RPGMobsPlugin plugin) {
        return plugin.getChargeLeapAbilityComponentType();
    }

    @Override
    protected ChargeLeapAbilityComponent getComponent(Store<EntityStore> store, Ref<EntityStore> ref,
                                                      RPGMobsPlugin plugin) {
        return store.getComponent(ref, plugin.getChargeLeapAbilityComponentType());
    }

    @Override
    protected ChargeLeapAbilityComponent createComponent(RPGMobsConfig.ChargeLeapAbilityConfig abilityConfig,
                                                         int tierIndex, boolean enabled, Random random) {
        var component = new ChargeLeapAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        return component;
    }
}
