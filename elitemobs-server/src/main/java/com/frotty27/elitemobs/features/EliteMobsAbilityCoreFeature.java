package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.components.ability.EliteMobsAbilityLockComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.ability.EliteMobsAbilityCombatReevaluationSystem;
import com.frotty27.elitemobs.systems.ability.EliteMobsAbilityCompletionSystem;
import com.frotty27.elitemobs.systems.ability.EliteMobsAbilityDamageSystem;
import com.frotty27.elitemobs.systems.ability.EliteMobsAbilityTriggerListener;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class EliteMobsAbilityCoreFeature implements IEliteMobsFeature {

    @Override
    public String getFeatureKey() {
        return "AbilityCore";
    }

    @Override
    public void apply(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
        EliteMobsAbilityLockComponent abilityLock = new EliteMobsAbilityLockComponent();
        commandBuffer.putComponent(npcRef, plugin.getAbilityLockComponentType(), abilityLock);
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        plugin.registerSystem(new EliteMobsAbilityDamageSystem(plugin));
        plugin.registerSystem(new EliteMobsAbilityCompletionSystem(plugin));

        EliteMobsAbilityTriggerListener triggerListener = new EliteMobsAbilityTriggerListener(plugin);
        plugin.getEventBus().registerListener(triggerListener);
        plugin.registerSystem(new EliteMobsAbilityCombatReevaluationSystem(plugin, triggerListener));
    }
}
