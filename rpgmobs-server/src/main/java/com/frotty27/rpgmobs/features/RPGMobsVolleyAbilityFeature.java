package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.ability.VolleyAbilityComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.AbilityIds;
import com.frotty27.rpgmobs.systems.ability.AbilityTriggerSource;
import com.frotty27.rpgmobs.systems.ability.TriggerContext;
import com.frotty27.rpgmobs.utils.AbilityHelpers;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class RPGMobsVolleyAbilityFeature
        extends AbstractGatedAbilityFeature<VolleyAbilityComponent, RPGMobsConfig.VolleyAbilityConfig> {

    @Override
    public String id() {
        return AbilityIds.VOLLEY;
    }

    @Override
    public String displayName() {
        return "Volley";
    }

    @Override
    public String description() {
        return "Equips a crossbow and fires a burst of arrows";
    }

    @Override
    public Set<AbilityTriggerSource> triggerSources() {
        return Set.of(AbilityTriggerSource.COMBAT_TICK, AbilityTriggerSource.AGGRO);
    }

    @Override
    public boolean canTrigger(TriggerContext context) {
        var volley = RPGMobsAbilityFeatureHelpers.getReadyAbilityComponent(
                context, context.plugin().getVolleyAbilityComponentType());
        if (volley == null) return false;

        RPGMobsCombatTrackingComponent combat = context.store().getComponent(
                context.entityRef(), context.plugin().getCombatTrackingComponentType()
        );
        if (combat == null || !combat.isInCombat()) return false;

        Ref<EntityStore> targetRef = combat.getBestTarget();
        if (targetRef == null || !targetRef.isValid()) return false;

        RPGMobsConfig.AbilityConfig rawConfig = context.config().abilitiesConfig.defaultAbilities.get(id());
        if (!(rawConfig instanceof RPGMobsConfig.VolleyAbilityConfig abilityConfig)) return false;

        float distance = AbilityHelpers.calculateDistance(context.entityRef(), targetRef, context.store());
        if (distance < abilityConfig.minRange || distance > abilityConfig.maxRange) return false;

        return ThreadLocalRandom.current().nextFloat() < volley.volleyTriggerChance;
    }

    @Override
    public void onPreChainStart(TriggerContext context, NPCEntity npcEntity) {
        RPGMobsConfig.AbilityConfig rawConfig = context.config().abilitiesConfig.defaultAbilities.get(id());
        if (!(rawConfig instanceof RPGMobsConfig.VolleyAbilityConfig volleyConfig)) return;

        VolleyAbilityComponent volley = context.store().getComponent(
                context.entityRef(), context.plugin().getVolleyAbilityComponentType()
        );
        if (volley == null) return;

        AbilityHelpers.swapToItemInHand(npcEntity, volley, volleyConfig.npcCrossbowItemId);
    }

    @Override
    public void onChainStartFailed(TriggerContext context, NPCEntity npcEntity) {
        VolleyAbilityComponent volley = context.store().getComponent(
                context.entityRef(), context.plugin().getVolleyAbilityComponentType()
        );
        if (volley == null) return;

        AbilityHelpers.restoreWeaponIfNeeded(npcEntity, volley);
    }

    @Override
    public RPGMobsConfig.AbilityConfig createDefaultConfig() {
        return new RPGMobsConfig.VolleyAbilityConfig();
    }

    @Override
    public List<AbilityConfigField> describeConfigFields() {
        return List.of(
                new AbilityConfigField.PerTierFloat("Trigger Chance",
                        config -> ((RPGMobsConfig.VolleyAbilityConfig) config).volleyTriggerChancePerTier,
                        (config, value) -> ((RPGMobsConfig.VolleyAbilityConfig) config).volleyTriggerChancePerTier = value),
                new AbilityConfigField.ScalarFloat("Min Range",
                        config -> ((RPGMobsConfig.VolleyAbilityConfig) config).minRange,
                        (config, value) -> ((RPGMobsConfig.VolleyAbilityConfig) config).minRange = value),
                new AbilityConfigField.ScalarFloat("Max Range",
                        config -> ((RPGMobsConfig.VolleyAbilityConfig) config).maxRange,
                        (config, value) -> ((RPGMobsConfig.VolleyAbilityConfig) config).maxRange = value),
                new AbilityConfigField.PerTierInt("Projectile Count",
                        config -> ((RPGMobsConfig.VolleyAbilityConfig) config).projectileCountPerTier,
                        (config, value) -> ((RPGMobsConfig.VolleyAbilityConfig) config).projectileCountPerTier = value),
                new AbilityConfigField.PerTierFloat("Spread Angle",
                        config -> ((RPGMobsConfig.VolleyAbilityConfig) config).spreadAnglePerTier,
                        (config, value) -> ((RPGMobsConfig.VolleyAbilityConfig) config).spreadAnglePerTier = value),
                new AbilityConfigField.PerTierInt("Base Damage Per Projectile",
                        config -> ((RPGMobsConfig.VolleyAbilityConfig) config).baseDamagePerProjectilePerTier,
                        (config, value) -> ((RPGMobsConfig.VolleyAbilityConfig) config).baseDamagePerProjectilePerTier = value),
                new AbilityConfigField.ScalarString("Crossbow Item ID",
                        config -> ((RPGMobsConfig.VolleyAbilityConfig) config).npcCrossbowItemId,
                        (config, value) -> ((RPGMobsConfig.VolleyAbilityConfig) config).npcCrossbowItemId = value)
        );
    }

    @Override
    protected Class<RPGMobsConfig.VolleyAbilityConfig> configClass() {
        return RPGMobsConfig.VolleyAbilityConfig.class;
    }

    @Override
    protected ComponentType<EntityStore, VolleyAbilityComponent> componentType(RPGMobsPlugin plugin) {
        return plugin.getVolleyAbilityComponentType();
    }

    @Override
    protected VolleyAbilityComponent getComponent(Store<EntityStore> store, Ref<EntityStore> ref,
                                                   RPGMobsPlugin plugin) {
        return store.getComponent(ref, plugin.getVolleyAbilityComponentType());
    }

    @Override
    protected VolleyAbilityComponent createComponent(RPGMobsConfig.VolleyAbilityConfig abilityConfig,
                                                      int tierIndex, boolean enabled, Random random) {
        float triggerChance = tierIndex < abilityConfig.volleyTriggerChancePerTier.length
                ? abilityConfig.volleyTriggerChancePerTier[tierIndex] : 0f;

        var component = new VolleyAbilityComponent();
        component.abilityEnabled = enabled;
        component.cooldownTicksRemaining = 0L;
        component.volleyTriggerChance = triggerChance;
        return component;
    }
}
