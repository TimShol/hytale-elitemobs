package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.AbilityEnabledComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityTriggerSource;
import com.frotty27.rpgmobs.systems.ability.TriggerContext;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;

public abstract class AbstractMultiSlashFeature<C extends AbilityEnabledComponent>
        extends AbstractGatedAbilityFeature<C, RPGMobsConfig.MultiSlashAbilityConfig> {

    public static final String VARIANT_SWORDS = "swords";
    public static final String VARIANT_LONGSWORDS = "longswords";
    public static final String VARIANT_DAGGERS = "daggers";
    public static final String VARIANT_BATTLEAXES = "battleaxes";
    public static final String VARIANT_AXES = "axes";
    public static final String VARIANT_MACES = "maces";
    public static final String VARIANT_CLUBS = "clubs";
    public static final String VARIANT_CLUBS_FLAIL = "clubsFlail";
    public static final String VARIANT_SPEARS = "spears";

    public static final String[] ALL_VARIANT_KEYS = {
            VARIANT_SWORDS, VARIANT_LONGSWORDS, VARIANT_DAGGERS, VARIANT_BATTLEAXES,
            VARIANT_AXES, VARIANT_MACES, VARIANT_CLUBS, VARIANT_SPEARS
    };

    public static final String[] ALL_VARIANT_LABELS = {
            "Swords", "Longswords", "Daggers", "Battleaxes", "Axes", "Maces", "Clubs", "Spears"
    };

    protected static final Map<String, String> CATEGORY_TO_VARIANT = Map.of(
            "Daggers", VARIANT_DAGGERS,
            "Battleaxes", VARIANT_BATTLEAXES,
            "Axes", VARIANT_AXES,
            "Maces", VARIANT_MACES,
            "Clubs", VARIANT_CLUBS,
            "Spears", VARIANT_SPEARS,
            "Longswords", VARIANT_LONGSWORDS,
            "Swords", VARIANT_SWORDS
    );

    protected abstract int variationCount();

    protected abstract String rootKeyFor(String variant, int variationIdx);

    protected abstract String getWeaponVariant(C component);

    protected abstract void setWeaponVariant(C component, String variant);

    protected abstract float getTriggerChance(C component);

    protected abstract void setTriggerChance(C component, float chance);

    @Override
    public Set<AbilityTriggerSource> triggerSources() {
        return Set.of(AbilityTriggerSource.COMBAT_TICK);
    }

    @Override
    public RPGMobsConfig.AbilityConfig createDefaultConfig() {
        return new RPGMobsConfig.MultiSlashAbilityConfig();
    }

    @Override
    public List<AbilityConfigField> describeConfigFields() {
        return List.of(
                new AbilityConfigField.PerTierFloat("Trigger Chance",
                        config -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).slashTriggerChancePerTier,
                        (config, value) -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).slashTriggerChancePerTier = value),
                new AbilityConfigField.ScalarFloat("Melee Range",
                        config -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).meleeRange,
                        (config, value) -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).meleeRange = value),
                new AbilityConfigField.PerTierInt("Base Damage Per Hit",
                        config -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).baseDamagePerHitPerTier,
                        (config, value) -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).baseDamagePerHitPerTier = value),
                new AbilityConfigField.PerTierFloat("Forward Drift Force",
                        config -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).forwardDriftForcePerTier,
                        (config, value) -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).forwardDriftForcePerTier = value),
                new AbilityConfigField.PerTierFloat("Knockback Force",
                        config -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).knockbackForcePerTier,
                        (config, value) -> ((RPGMobsConfig.MultiSlashAbilityConfig) config).knockbackForcePerTier = value)
        );
    }

    @Override
    protected Class<RPGMobsConfig.MultiSlashAbilityConfig> configClass() {
        return RPGMobsConfig.MultiSlashAbilityConfig.class;
    }

    @Override
    public boolean canTrigger(TriggerContext context) {
        C comp = getComponent(context.store(), context.entityRef(), context.plugin());
        if (comp == null || !comp.isAbilityEnabled() || comp.getCooldownTicksRemaining() > 0) return false;

        RPGMobsCombatTrackingComponent combat = context.store().getComponent(
                context.entityRef(), context.plugin().getCombatTrackingComponentType()
        );
        if (combat == null || !combat.isInCombat()) return false;

        Ref<EntityStore> targetRef = combat.getBestTarget();
        if (targetRef == null || !targetRef.isValid()) return false;

        RPGMobsConfig.AbilityConfig rawConfig = context.config().abilitiesConfig.defaultAbilities.get(id());
        if (!(rawConfig instanceof RPGMobsConfig.MultiSlashAbilityConfig abilityConfig)) return false;

        RPGMobsConfig.MultiSlashVariantConfig variantCfg = abilityConfig.getVariantOrDefault(getWeaponVariant(comp));
        float distance = calculateDistance(context.entityRef(), targetRef, context.store());
        if (distance > variantCfg.meleeRange) return false;

        return ThreadLocalRandom.current().nextFloat() < getTriggerChance(comp);
    }

    @Override
    protected void populateComponent(C component, RPGMobsPlugin plugin,
                                     Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                     int tierIndex) {
        setWeaponVariant(component, resolveWeaponVariant(plugin, npcRef, entityStore));

        RPGMobsConfig config = plugin.getConfig();
        if (config != null) {
            RPGMobsConfig.AbilityConfig rawCfg = config.abilitiesConfig.defaultAbilities.get(id());
            if (rawCfg instanceof RPGMobsConfig.MultiSlashAbilityConfig slashCfg) {
                RPGMobsConfig.MultiSlashVariantConfig variantCfg = slashCfg.getVariantOrDefault(getWeaponVariant(component));
                int clamped = clampTierIndex(tierIndex);
                float chance = clamped < variantCfg.slashTriggerChancePerTier.length
                        ? variantCfg.slashTriggerChancePerTier[clamped] : 0f;
                setTriggerChance(component, chance);
            }
        }
    }

    @Override
    public String resolveRootTemplateKey(TriggerContext context) {
        String variant = resolveWeaponVariant(context.plugin(), context.entityRef(), context.store());
        int variation = variationCount() <= 1 ? 0 : ThreadLocalRandom.current().nextInt(variationCount());
        String rootKey = rootKeyFor(variant, variation);
        RPGMobsLogger.debug(LOGGER, "[%s] resolveRoot: variant=%s variation=%d -> rootKey=%s",
                RPGMobsLogLevel.INFO, id(), variant, variation, rootKey);
        return rootKey;
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    protected String resolveWeaponVariant(RPGMobsPlugin plugin, Ref<EntityStore> npcRef,
                                          Store<EntityStore> entityStore) {
        String weaponId = RPGMobsAbilityFeatureHelpers.resolveWeaponId(npcRef, entityStore);
        if (weaponId.isEmpty()) {
            RPGMobsLogger.debug(LOGGER, "[WeaponVariant] weaponId empty, defaulting to swords",
                    RPGMobsLogLevel.INFO);
            return VARIANT_SWORDS;
        }

        RPGMobsConfig config = plugin.getConfig();
        if (config == null || config.gearConfig == null || config.gearConfig.weaponCategoryTree == null) {
            RPGMobsLogger.debug(LOGGER, "[WeaponVariant] no config/gearConfig, defaulting to swords",
                    RPGMobsLogLevel.INFO);
            return VARIANT_SWORDS;
        }

        RPGMobsConfig.GearCategory weaponTree = config.gearConfig.weaponCategoryTree;
        for (RPGMobsConfig.GearCategory category : weaponTree.children) {
            if (category.itemKeys.contains(weaponId)) {
                String variant = CATEGORY_TO_VARIANT.get(category.name);
                if (variant != null) {
                    if (VARIANT_CLUBS.equals(variant) && weaponId.toLowerCase().contains("flail")) {
                        RPGMobsLogger.debug(LOGGER,
                                "[WeaponVariant] weaponId=%s -> category=%s -> variant=clubsFlail",
                                RPGMobsLogLevel.INFO, weaponId, category.name);
                        return VARIANT_CLUBS_FLAIL;
                    }
                    RPGMobsLogger.debug(LOGGER,
                            "[WeaponVariant] weaponId=%s -> category=%s -> variant=%s",
                            RPGMobsLogLevel.INFO, weaponId, category.name, variant);
                    return variant;
                }
                RPGMobsLogger.debug(LOGGER,
                        "[WeaponVariant] weaponId=%s found in category=%s but no variant mapping",
                        RPGMobsLogLevel.WARNING, weaponId, category.name);
            }
        }

        RPGMobsLogger.debug(LOGGER,
                "[WeaponVariant] weaponId=%s not found in any category, defaulting to swords",
                RPGMobsLogLevel.WARNING, weaponId);
        return VARIANT_SWORDS;
    }

    protected float calculateDistance(Ref<EntityStore> entityRef, Ref<EntityStore> targetRef, Store<EntityStore> store) {
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
}
