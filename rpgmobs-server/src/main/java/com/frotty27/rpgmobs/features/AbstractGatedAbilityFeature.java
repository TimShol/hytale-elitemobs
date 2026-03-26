package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.AbilityEnabledComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.rules.AbilityGateEvaluator;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.Random;

public abstract class AbstractGatedAbilityFeature<
        C extends AbilityEnabledComponent,
        A extends RPGMobsConfig.AbilityConfig>
        implements IRPGMobsAbilityFeature {

    private final Random random = new Random();

    protected abstract Class<A> configClass();

    protected abstract ComponentType<EntityStore, C> componentType(RPGMobsPlugin plugin);

    protected abstract C getComponent(Store<EntityStore> store, Ref<EntityStore> ref, RPGMobsPlugin plugin);

    protected abstract C createComponent(A abilityConfig, int tierIndex, boolean enabled, Random random);

    protected void populateComponent(C component, RPGMobsPlugin plugin,
                                     Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                     int tierIndex) {
    }

    protected void onDisable(C component) {
    }

    protected void afterApply(RPGMobsPlugin plugin, Ref<EntityStore> npcRef,
                              Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
    }

    protected void afterRemove(RPGMobsPlugin plugin, Ref<EntityStore> npcRef,
                               Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public final void cleanup(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                              Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        commandBuffer.tryRemoveComponent(npcRef, componentType(plugin));
        afterRemove(plugin, npcRef, entityStore, commandBuffer);
    }

    @Override
    public final void apply(RPGMobsPlugin plugin, RPGMobsConfig config, ResolvedConfig resolved,
                            Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                            CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent,
                            @Nullable String roleName) {
        RPGMobsConfig.AbilityConfig rawConfig = config.abilitiesConfig.defaultAbilities.get(id());
        if (!configClass().isInstance(rawConfig)) return;

        A abilityConfig = configClass().cast(rawConfig);

        int tierIndex = tierComponent.tierIndex;
        String weaponId = RPGMobsAbilityFeatureHelpers.resolveWeaponId(npcRef, entityStore);
        String matchedRuleKey = tierComponent.matchedRuleKey;

        if (!AbilityGateEvaluator.isAllowed(abilityConfig, id(), weaponId, tierIndex, matchedRuleKey, resolved)) return;

        if (!isAllowedForMinions()) {
            RPGMobsSummonedMinionComponent minion = entityStore.getComponent(
                    npcRef, plugin.getSummonedMinionComponentType());
            if (minion != null) return;
        }

        float spawnChance = tierIndex < abilityConfig.chancePerTier.length
                ? abilityConfig.chancePerTier[tierIndex] : 0f;

        boolean enabled = random.nextFloat() < spawnChance;

        C component = createComponent(abilityConfig, tierIndex, enabled, random);
        populateComponent(component, plugin, npcRef, entityStore, tierIndex);
        commandBuffer.putComponent(npcRef, componentType(plugin), component);

        afterApply(plugin, npcRef, entityStore, commandBuffer);
    }

    @Override
    public final void reconcile(RPGMobsPlugin plugin, RPGMobsConfig config, ResolvedConfig resolved,
                                Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                                CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent,
                                @Nullable String roleName) {
        C component = getComponent(entityStore, npcRef, plugin);

        RPGMobsConfig.AbilityConfig rawConfig = config.abilitiesConfig.defaultAbilities.get(id());
        A abilityConfig = configClass().isInstance(rawConfig) ? configClass().cast(rawConfig) : null;

        if (abilityConfig == null) {
            if (component != null && component.isAbilityEnabled()) {
                component.setAbilityEnabled(false);
                onDisable(component);
                commandBuffer.replaceComponent(npcRef, componentType(plugin), component);
            }
            return;
        }

        int tierIndex = tierComponent.tierIndex;
        String weaponId = RPGMobsAbilityFeatureHelpers.resolveWeaponId(npcRef, entityStore);
        String matchedRuleKey = tierComponent.matchedRuleKey;

        boolean allowed = AbilityGateEvaluator.isAllowed(
                abilityConfig, id(), weaponId, tierIndex, matchedRuleKey, resolved);

        if (allowed && !isAllowedForMinions()) {
            RPGMobsSummonedMinionComponent minion = entityStore.getComponent(
                    npcRef, plugin.getSummonedMinionComponentType());
            if (minion != null) allowed = false;
        }

        if (component == null) {
            if (allowed) {
                apply(plugin, config, resolved, npcRef, entityStore, commandBuffer, tierComponent, roleName);
            }
            return;
        }

        if (!allowed && component.isAbilityEnabled()) {
            component.setAbilityEnabled(false);
            onDisable(component);
            commandBuffer.replaceComponent(npcRef, componentType(plugin), component);
        } else if (allowed && !component.isAbilityEnabled()) {
            apply(plugin, config, resolved, npcRef, entityStore, commandBuffer, tierComponent, roleName);
        } else if (allowed && component.isAbilityEnabled()) {
            afterApply(plugin, npcRef, entityStore, commandBuffer);
        }
    }
}
