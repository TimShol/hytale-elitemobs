package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.assets.AssetConfigHelpers;
import com.frotty27.elitemobs.assets.AssetType;
import com.frotty27.elitemobs.components.EliteMobsLeapAbilityStateComponent;
import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.frotty27.elitemobs.systems.ability.EliteMobsLeapAbilitySystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import static com.frotty27.elitemobs.config.EliteMobsConfig.ABILITY_CHARGE_LEAP_KEY;
import static com.frotty27.elitemobs.utils.ClampingHelpers.clampTierIndex;

public final class LeapAbilityFeature implements EliteMobsFeature {

    @Override
    public String id() {
        return "ability_charge_leap";
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
        reconcile(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
    }

    @Override
    public void reconcile(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
        ComponentType<EntityStore, EliteMobsLeapAbilityStateComponent> leapAbilityComponentType =
                plugin.getLeapAbilityComponent();
        if (leapAbilityComponentType == null) return;

        int tierIndex = clampTierIndex(tierComponent.tierIndex);

        boolean wantsLeapAbility = AssetConfigHelpers.isAssetConfigEnabledForTier(
                AssetConfigHelpers.getAssetConfig(config, AssetType.ABILITIES, ABILITY_CHARGE_LEAP_KEY),
                tierIndex
        );

        EliteMobsLeapAbilityStateComponent existingLeapState =
                entityStore.getComponent(npcRef, leapAbilityComponentType);

        if (wantsLeapAbility) {
            if (existingLeapState == null) {
                commandBuffer.putComponent(npcRef, leapAbilityComponentType, new EliteMobsLeapAbilityStateComponent());
            }
            return;
        }

        if (existingLeapState != null) {
            try {
                commandBuffer.removeComponent(npcRef, leapAbilityComponentType);
            } catch (IllegalArgumentException ignored) {
                // Skip removal if the archetype no longer contains this component type.
            }
        }
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        // We do NOT register here yet because it is already registered manually in Plugin.
        // Once we remove manual registration, we can uncomment this.
        // plugin.registerSystem(new EliteMobsLeapAbilitySystem(plugin));
    }
}