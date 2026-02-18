package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.ability.RPGMobsAbilityLockComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.ability.RPGMobsAbilityCombatReevaluationSystem;
import com.frotty27.rpgmobs.systems.ability.RPGMobsAbilityCompletionSystem;
import com.frotty27.rpgmobs.systems.ability.RPGMobsAbilityDamageSystem;
import com.frotty27.rpgmobs.systems.ability.RPGMobsAbilityTriggerListener;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsAbilityCoreFeature implements IRPGMobsFeature {

    @Override
    public String getFeatureKey() {
        return "AbilityCore";
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        RPGMobsAbilityLockComponent abilityLock = new RPGMobsAbilityLockComponent();
        commandBuffer.putComponent(npcRef, plugin.getAbilityLockComponentType(), abilityLock);
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
        plugin.registerSystem(new RPGMobsAbilityDamageSystem(plugin));
        plugin.registerSystem(new RPGMobsAbilityCompletionSystem(plugin));

        RPGMobsAbilityTriggerListener triggerListener = new RPGMobsAbilityTriggerListener(plugin);
        plugin.getEventBus().registerListener(triggerListener);
        plugin.registerSystem(new RPGMobsAbilityCombatReevaluationSystem(plugin, triggerListener));
    }
}
