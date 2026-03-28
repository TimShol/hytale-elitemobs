package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.AbilityEnabledComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.logs.RPGMobsLogLevel;
import com.frotty27.rpgmobs.logs.RPGMobsLogger;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityTriggerSource;
import com.frotty27.rpgmobs.systems.ability.TriggerContext;
import com.frotty27.rpgmobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;

public abstract class AbstractMultiSlashFeature<C extends AbilityEnabledComponent>
        extends AbstractGatedAbilityFeature<C, RPGMobsConfig.MultiSlashAbilityConfig> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
        float distance = AbilityHelpers.calculateDistance(context.entityRef(), targetRef, context.store());
        if (distance > variantCfg.meleeRange) return false;

        return ThreadLocalRandom.current().nextFloat() < getTriggerChance(comp);
    }

    @Override
    protected void populateComponent(C component, RPGMobsPlugin plugin,
                                     Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                     int tierIndex) {
        setWeaponVariant(component, AbilityHelpers.resolveWeaponVariant(plugin, npcRef, entityStore));

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
        String variant = AbilityHelpers.resolveWeaponVariant(context.plugin(), context.entityRef(), context.store());
        int variation = variationCount() <= 1 ? 0 : ThreadLocalRandom.current().nextInt(variationCount());
        String rootKey = rootKeyFor(variant, variation);
        RPGMobsLogger.debug(LOGGER, "[%s] resolveRoot: variant=%s variation=%d -> rootKey=%s",
                RPGMobsLogLevel.INFO, id(), variant, variation, rootKey);
        return rootKey;
    }

}
