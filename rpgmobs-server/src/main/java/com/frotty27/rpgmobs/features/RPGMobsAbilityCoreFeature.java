package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.*;
import com.frotty27.rpgmobs.systems.combat.RPGMobsPlayerAttackDetectionSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsAbilityCoreFeature implements IRPGMobsFeature {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public String getFeatureKey() {
        return "AbilityCore";
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, ResolvedConfig resolved,
                      Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                      CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent,
                      @Nullable String roleName) {
        commandBuffer.putComponent(npcRef, plugin.getAbilityLockComponentType(),
                new RPGMobsAbilityLockComponent());

        // Register for code-side reactive parry (T2+ can parry)
        if (tierComponent.tierIndex >= 1) {
            plugin.getPlayerAttackTracker().registerGuardEntity(npcRef);
        }
    }

    @Override
    public void reconcile(RPGMobsPlugin plugin, RPGMobsConfig config, ResolvedConfig resolved,
                          Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                          CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent,
                          @Nullable String roleName) {
        RPGMobsAbilityLockComponent existingLock = entityStore.getComponent(npcRef,
                plugin.getAbilityLockComponentType());
        if (existingLock == null) {
            commandBuffer.putComponent(npcRef, plugin.getAbilityLockComponentType(),
                    new RPGMobsAbilityLockComponent());
        }

        RPGMobsCombatTrackingComponent existingCombat = entityStore.getComponent(npcRef,
                plugin.getCombatTrackingComponentType());
        if (existingCombat == null) {
            commandBuffer.putComponent(npcRef, plugin.getCombatTrackingComponentType(),
                    new RPGMobsCombatTrackingComponent());
        }
    }

    @Override
    public void cleanup(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                        Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        commandBuffer.tryRemoveComponent(npcRef, plugin.getAbilityLockComponentType());
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
        var damageSystem = new RPGMobsAbilityDamageSystem(plugin);
        plugin.registerSystem(damageSystem);
        plugin.registerSystem(new RPGMobsAbilityCompletionSystem(plugin));

        var triggerListener = new RPGMobsAbilityTriggerListener(plugin);
        plugin.getEventBus().registerListener(triggerListener);
        plugin.registerSystem(new RPGMobsAbilityCombatReevaluationSystem(plugin, triggerListener));
        plugin.registerSystem(new RPGMobsAbilityCombatTickSystem(plugin, triggerListener));
        plugin.registerSystem(new RPGMobsPlayerAttackDetectionSystem(plugin));
        plugin.getPlayerAttackTracker().initialize(plugin, triggerListener);
    }
}
