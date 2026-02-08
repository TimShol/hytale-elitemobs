package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.logs.EliteMobsLogLevel;
import com.frotty27.elitemobs.logs.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class EliteMobsNameplateFeature implements EliteMobsFeature {

    @Override
    public String getFeatureKey() {
        return "Nameplate";
    }

    @Override
    public Object getConfig(EliteMobsConfig config) {
        return config.nameplatesConfig;
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
        if (roleName == null || roleName.isBlank()) {
            if (config != null && !config.nameplatesConfig.nameplatesEnabled) {
                plugin.getNameplateService().applyOrUpdateNameplate(config, npcRef, entityStore, commandBuffer, "", tierComponent.tierIndex);
            }
            return;
        }
        plugin.getNameplateService().applyOrUpdateNameplate(config, npcRef, entityStore, commandBuffer, roleName, tierComponent.tierIndex);
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
        if (config != null && config.debugConfig != null && config.debugConfig.isDebugModeEnabled) {
           EliteMobsLogger.debug(
                   HytaleLogger.forEnclosingClass(),
                   "[Nameplate] reconcile nameplatesEnabled=%s perTier=%s role=%s tier=%d",
                   EliteMobsLogLevel.INFO,
                   String.valueOf(config.nameplatesConfig.nameplatesEnabled),
                   Arrays.toString(config.nameplatesConfig.nameplatesEnabledPerTier),
                   String.valueOf(roleName),
                   tierComponent.tierIndex
            );
        }
        if (roleName == null || roleName.isBlank()) {
            if (config != null && !config.nameplatesConfig.nameplatesEnabled) {
                plugin.getNameplateService().applyOrUpdateNameplate(config, npcRef, entityStore, commandBuffer, "", tierComponent.tierIndex);
            }
            return;
        }
        plugin.getNameplateService().applyOrUpdateNameplate(config, npcRef, entityStore, commandBuffer, roleName, tierComponent.tierIndex);
    }
}
