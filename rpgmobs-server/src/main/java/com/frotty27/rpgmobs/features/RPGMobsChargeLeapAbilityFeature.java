package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.ChargeLeapAbilityComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.frotty27.rpgmobs.systems.ability.AbilityTriggerSource;
import com.frotty27.rpgmobs.systems.ability.TriggerContext;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Random;
import java.util.Set;

public final class RPGMobsChargeLeapAbilityFeature
        extends AbstractGatedAbilityFeature<ChargeLeapAbilityComponent, RPGMobsConfig.ChargeLeapAbilityConfig> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public String id() {
        return AbilityIds.CHARGE_LEAP;
    }

    @Override
    public String displayName() {
        return "Charge Leap";
    }

    @Override
    public String description() {
        return "Gap-closing charge + aerial slam attack";
    }

    @Override
    public Set<AbilityTriggerSource> triggerSources() {
        return Set.of(AbilityTriggerSource.AGGRO);
    }

    @Override
    public boolean canTrigger(TriggerContext context) {
        var chargeLeap = RPGMobsAbilityFeatureHelpers.getReadyAbilityComponent(
                context, context.plugin().getChargeLeapAbilityComponentType());
        if (chargeLeap == null) return false;

        RPGMobsCombatTrackingComponent combat = context.store().getComponent(
                context.entityRef(), context.plugin().getCombatTrackingComponentType()
        );
        if (combat == null || !combat.isInCombat()) return false;

        Ref<EntityStore> targetRef = combat.getBestTarget();
        if (targetRef == null || !targetRef.isValid()) return false;
        if (combat.aiTarget == null || !combat.aiTarget.isValid()) return false;

        RPGMobsConfig.AbilityConfig rawConfig = context.config().abilitiesConfig.defaultAbilities.get(id());
        if (!(rawConfig instanceof RPGMobsConfig.ChargeLeapAbilityConfig abilityConfig)) return false;

        float distance = calculateDistance(context.entityRef(), targetRef, context.store());
        boolean inRange = distance >= abilityConfig.minRange && distance <= abilityConfig.maxRange;
        RPGMobsLogger.debug(LOGGER,
                "[ChargeLeap] canTrigger: distance=%.1f minRange=%.1f maxRange=%.1f inRange=%b",
                RPGMobsLogLevel.INFO, distance, abilityConfig.minRange, abilityConfig.maxRange, inRange);
        return inRange;
    }

    @Override
    protected void populateComponent(ChargeLeapAbilityComponent component, RPGMobsPlugin plugin,
                                     Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                     int tierIndex) {
        component.weaponVariant = resolveWeaponVariant(plugin, npcRef, entityStore);
    }

    @Override
    public String resolveRootTemplateKey(TriggerContext context) {
        String variant = resolveWeaponVariant(context.plugin(), context.entityRef(), context.store());
        String cap = Character.toUpperCase(variant.charAt(0)) + variant.substring(1);
        return "root" + cap;
    }

    private String resolveWeaponVariant(RPGMobsPlugin plugin, Ref<EntityStore> npcRef,
                                        Store<EntityStore> entityStore) {
        String weaponId = RPGMobsAbilityFeatureHelpers.resolveWeaponId(npcRef, entityStore);
        if (weaponId.isEmpty()) return AbstractMultiSlashFeature.VARIANT_SWORDS;

        RPGMobsConfig config = plugin.getConfig();
        if (config == null || config.gearConfig == null || config.gearConfig.weaponCategoryTree == null) {
            return AbstractMultiSlashFeature.VARIANT_SWORDS;
        }

        RPGMobsConfig.GearCategory weaponTree = config.gearConfig.weaponCategoryTree;
        for (RPGMobsConfig.GearCategory category : weaponTree.children) {
            if (category.itemKeys.contains(weaponId)) {
                String mapped = AbstractMultiSlashFeature.CATEGORY_TO_VARIANT.get(category.name);
                if (mapped != null) {
                    if (AbstractMultiSlashFeature.VARIANT_CLUBS.equals(mapped)
                            && weaponId.toLowerCase().contains("flail")) {
                        return AbstractMultiSlashFeature.VARIANT_CLUBS_FLAIL;
                    }
                    return mapped;
                }
            }
        }

        return AbstractMultiSlashFeature.VARIANT_SWORDS;
    }

    private float calculateDistance(Ref<EntityStore> entityRef, Ref<EntityStore> targetRef, Store<EntityStore> store) {
        TransformComponent mobTransform = store.getComponent(entityRef, TransformComponent.getComponentType());
        TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (mobTransform == null || targetTransform == null) return Float.MAX_VALUE;

        Vector3d mobPos = mobTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition();
        double dx = targetPos.getX() - mobPos.getX();
        double dy = targetPos.getY() - mobPos.getY();
        double dz = targetPos.getZ() - mobPos.getZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
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
