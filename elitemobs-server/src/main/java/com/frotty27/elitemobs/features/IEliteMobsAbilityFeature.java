package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public interface IEliteMobsAbilityFeature extends IEliteMobsFeature {
    String id();

    @Override
    default String getFeatureKey() {
        return "EliteMobs.Ability." + id();
    }

    @Override
    default String getAssetId() {
        return id();
    }

    @Override
    default Object getConfig(EliteMobsConfig config) {
        return config.abilitiesConfig.defaultAbilities.get(id());
    }

    @Override
    default void apply(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> npcRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable String roleName
    ) {
        
    }
}
