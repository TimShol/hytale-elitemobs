package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.HealLeapAbilityComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.frotty27.rpgmobs.systems.ability.AbilityTriggerSource;
import com.frotty27.rpgmobs.systems.ability.TriggerContext;
import com.frotty27.rpgmobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.List;
import java.util.Random;
import java.util.Set;

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
    public String description() {
        return "Emergency heal potion + backward leap";
    }

    @Override
    public Set<AbilityTriggerSource> triggerSources() {
        return Set.of(AbilityTriggerSource.DAMAGE_RECEIVED, AbilityTriggerSource.COMBAT_TICK);
    }

    @Override
    public boolean canTrigger(TriggerContext context) {
        var healLeap = RPGMobsAbilityFeatureHelpers.getReadyAbilityComponent(
                context, context.plugin().getHealLeapAbilityComponentType());
        if (healLeap == null) return false;

        if (!isMobTypeAllowed(context)) return false;

        float healthPercent = calculateHealthPercent(context.entityRef(), context.store());

        if (context.source() == AbilityTriggerSource.COMBAT_TICK) {
            RPGMobsConfig.AbilityConfig rawConfig = context.config().abilitiesConfig.defaultAbilities.get(id());
            if (!(rawConfig instanceof RPGMobsConfig.HealLeapAbilityConfig healConfig)) return false;

            if (healthPercent >= healConfig.standingHealMaxHealthTriggerPercent) return false;

            double distanceSquared = getDistanceToTargetSq(context);
            if (distanceSquared < (double) healConfig.standingHealMinDistance * healConfig.standingHealMinDistance) {
                return false;
            }

            return true;
        }

        return healthPercent < healLeap.triggerHealthPercent;
    }

    private boolean isMobTypeAllowed(TriggerContext context) {
        RPGMobsConfig.AbilityConfig rawConfig = context.config().abilitiesConfig.defaultAbilities.get(id());
        if (!(rawConfig instanceof RPGMobsConfig.HealLeapAbilityConfig healConfig)) return true;

        RPGMobsTierComponent tier = context.store().getComponent(
                context.entityRef(), context.plugin().getRPGMobsComponentType()
        );
        if (tier == null || tier.matchedRuleKey == null || tier.matchedRuleKey.isBlank()) return true;

        String ruleKey = tier.matchedRuleKey;
        String ruleKeyLower = ruleKey.toLowerCase();

        for (String exception : healConfig.allowedExceptions) {
            if (ruleKeyLower.startsWith(exception.toLowerCase())) return true;
        }

        for (String denied : healConfig.deniedMobPrefixes) {
            if (ruleKeyLower.startsWith(denied.toLowerCase())) return false;
        }

        return true;
    }

    @Override
    public void onPreChainStart(TriggerContext context, NPCEntity npcEntity) {
        HealLeapAbilityComponent healLeap = context.store().getComponent(
                context.entityRef(), context.plugin().getHealLeapAbilityComponentType()
        );
        if (healLeap == null) return;

        RPGMobsConfig.AbilityConfig rawConfig = context.config().abilitiesConfig.defaultAbilities.get(id());
        if (!(rawConfig instanceof RPGMobsConfig.HealLeapAbilityConfig healConfig)) return;

        String potionItemId = healConfig.npcDrinkItemId;
        if (potionItemId == null || potionItemId.isBlank()) {
            potionItemId = "Potion_Health_Greater";
        }
        AbilityHelpers.swapToItemInHand(npcEntity, healLeap, potionItemId);
    }

    @Override
    public void onChainStartFailed(TriggerContext context, NPCEntity npcEntity) {
        HealLeapAbilityComponent healLeap = context.store().getComponent(
                context.entityRef(), context.plugin().getHealLeapAbilityComponentType()
        );
        AbilityHelpers.restoreWeaponIfNeeded(npcEntity, healLeap);
    }

    private double getDistanceToTargetSq(TriggerContext context) {
        Ref<EntityStore> targetRef = context.targetRef();
        if (targetRef == null || !targetRef.isValid()) return Double.MAX_VALUE;

        TransformComponent npcTransform = context.store().getComponent(
                context.entityRef(), TransformComponent.getComponentType());
        TransformComponent targetTransform = context.store().getComponent(
                targetRef, TransformComponent.getComponentType());

        if (npcTransform == null || targetTransform == null) return Double.MAX_VALUE;

        Vector3d npcPos = npcTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition();
        double dx = npcPos.getX() - targetPos.getX();
        double dy = npcPos.getY() - targetPos.getY();
        double dz = npcPos.getZ() - targetPos.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private float calculateHealthPercent(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStats = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStats == null) return 1.0f;

        int healthStatId = DefaultEntityStatTypes.getHealth();
        var healthStatValue = entityStats.get(healthStatId);
        if (healthStatValue == null) return 1.0f;

        float current = healthStatValue.get();
        float max = healthStatValue.getMax();
        if (max <= 0) return 1.0f;
        return current / max;
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
                new AbilityConfigField.ScalarInt("Interrupt Hit Count",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).interruptHitCount,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).interruptHitCount = value),
                new AbilityConfigField.PerTierFloat("Apply Force",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).applyForcePerTier,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).applyForcePerTier = value),
                new AbilityConfigField.StringList("Denied Mob Prefixes",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).deniedMobPrefixes,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).deniedMobPrefixes = value),
                new AbilityConfigField.StringList("Allowed Exceptions",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).allowedExceptions,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).allowedExceptions = value),
                new AbilityConfigField.ScalarFloat("Retreat Heal Min Distance",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).retreatHealMinDistance,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).retreatHealMinDistance = value),
                new AbilityConfigField.PerTierFloat("Standing Heal % Per Tier",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).standingHealPercentPerTier,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).standingHealPercentPerTier = value),
                new AbilityConfigField.ScalarFloat("Standing Heal Min Distance",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).standingHealMinDistance,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).standingHealMinDistance = value),
                new AbilityConfigField.ScalarFloat("Standing Heal Max Health %",
                        config -> ((RPGMobsConfig.HealLeapAbilityConfig) config).standingHealMaxHealthTriggerPercent,
                        (config, value) -> ((RPGMobsConfig.HealLeapAbilityConfig) config).standingHealMaxHealthTriggerPercent = value)
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
    public String resolveRootTemplateKey(TriggerContext context) {
        RPGMobsConfig.AbilityConfig rawConfig = context.config().abilitiesConfig.defaultAbilities.get(id());
        if (rawConfig instanceof RPGMobsConfig.HealLeapAbilityConfig healConfig) {
            if (context.source() == AbilityTriggerSource.COMBAT_TICK) {
                return RPGMobsConfig.HealLeapAbilityConfig.TEMPLATE_STANDING_HEAL_ROOT;
            }

            double distanceSquared = getDistanceToTargetSq(context);
            double minDistance = healConfig.standingHealMinDistance;
            if (distanceSquared >= minDistance * minDistance) {
                return RPGMobsConfig.HealLeapAbilityConfig.TEMPLATE_STANDING_HEAL_ROOT;
            }
        }
        return RPGMobsConfig.AbilityConfig.TEMPLATE_ROOT_INTERACTION;
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
