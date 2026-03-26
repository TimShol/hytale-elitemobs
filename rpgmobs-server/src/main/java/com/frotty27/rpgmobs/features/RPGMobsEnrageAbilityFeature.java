package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.EnrageAbilityComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.frotty27.rpgmobs.systems.ability.AbilityTriggerSource;
import com.frotty27.rpgmobs.systems.ability.TriggerContext;
import com.frotty27.rpgmobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class RPGMobsEnrageAbilityFeature
        extends AbstractGatedAbilityFeature<EnrageAbilityComponent, RPGMobsConfig.EnrageAbilityConfig> {

    @Override
    public String id() {
        return AbilityIds.ENRAGE;
    }

    @Override
    public String displayName() {
        return "Enrage";
    }

    @Override
    public String description() {
        return "Berserk fist-fighting on low HP  - de-equips weapon, punches for 20s, then dies";
    }

    @Override
    public Set<AbilityTriggerSource> triggerSources() {
        return Set.of(AbilityTriggerSource.DAMAGE_RECEIVED);
    }

    @Override
    public String resolveRootTemplateKey(TriggerContext context) {
        int variation = ThreadLocalRandom.current().nextInt(3);
        return switch (variation) {
            case 0 -> "rootV1";
            case 1 -> "rootV2";
            case 2 -> "rootV3";
            default -> super.resolveRootTemplateKey(context);
        };
    }

    @Override
    public void onPreChainStart(TriggerContext context, NPCEntity npcEntity) {
        var enrage = context.store().getComponent(context.entityRef(), context.plugin().getEnrageAbilityComponentType());
        if (enrage != null) {
            AbilityHelpers.unequipWeaponInHand(npcEntity, enrage);
            AbilityHelpers.unequipUtilitySlotForEnrage(npcEntity, enrage);
        }
    }

    @Override
    public void onChainStartFailed(TriggerContext context, NPCEntity npcEntity) {
        var enrage = context.store().getComponent(context.entityRef(), context.plugin().getEnrageAbilityComponentType());
        if (enrage != null) {
            AbilityHelpers.restoreWeaponIfNeeded(npcEntity, enrage);
            AbilityHelpers.restoreEnrageUtilityIfNeeded(npcEntity, enrage);
        }
    }

    @Override
    public boolean canTrigger(TriggerContext context) {
        var enrage = RPGMobsAbilityFeatureHelpers.getReadyAbilityComponent(
                context, context.plugin().getEnrageAbilityComponentType());
        if (enrage == null) return false;

        if (enrage.enraged) return false;

        float healthPercent = calculateHealthPercent(context.entityRef(), context.store());
        return healthPercent <= enrage.triggerHealthPercent;
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
        return new RPGMobsConfig.EnrageAbilityConfig();
    }

    @Override
    public List<AbilityConfigField> describeConfigFields() {
        return List.of(
                new AbilityConfigField.PerTierFloat("Trigger Health %",
                        config -> ((RPGMobsConfig.EnrageAbilityConfig) config).triggerHealthPercentPerTier,
                        (config, value) -> ((RPGMobsConfig.EnrageAbilityConfig) config).triggerHealthPercentPerTier = value),
                new AbilityConfigField.PerTierFloat("Light Punch Damage",
                        config -> ((RPGMobsConfig.EnrageAbilityConfig) config).lightPunchDamagePerTier,
                        (config, value) -> ((RPGMobsConfig.EnrageAbilityConfig) config).lightPunchDamagePerTier = value),
                new AbilityConfigField.PerTierFloat("Heavy Punch Damage",
                        config -> ((RPGMobsConfig.EnrageAbilityConfig) config).heavyPunchDamagePerTier,
                        (config, value) -> ((RPGMobsConfig.EnrageAbilityConfig) config).heavyPunchDamagePerTier = value)
        );
    }

    @Override
    protected Class<RPGMobsConfig.EnrageAbilityConfig> configClass() {
        return RPGMobsConfig.EnrageAbilityConfig.class;
    }

    @Override
    protected ComponentType<EntityStore, EnrageAbilityComponent> componentType(RPGMobsPlugin plugin) {
        return plugin.getEnrageAbilityComponentType();
    }

    @Override
    protected EnrageAbilityComponent getComponent(Store<EntityStore> store, Ref<EntityStore> ref,
                                                   RPGMobsPlugin plugin) {
        return store.getComponent(ref, plugin.getEnrageAbilityComponentType());
    }

    @Override
    protected EnrageAbilityComponent createComponent(RPGMobsConfig.EnrageAbilityConfig abilityConfig,
                                                      int tierIndex, boolean enabled, Random random) {
        float triggerHealth = tierIndex < abilityConfig.triggerHealthPercentPerTier.length
                ? abilityConfig.triggerHealthPercentPerTier[tierIndex] : 0.3f;

        var component = new EnrageAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        component.triggerHealthPercent = triggerHealth;
        component.enraged = false;
        return component;
    }
}
