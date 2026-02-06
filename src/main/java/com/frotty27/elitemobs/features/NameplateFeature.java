package com.frotty27.elitemobs.features;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.log.EliteMobsLogLevel;
import com.frotty27.elitemobs.log.EliteMobsLogger;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class NameplateFeature implements EliteMobsFeature {

    @Override
    public String id() {
        return "nameplate";
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
            if (config != null && !config.nameplates.nameplatesEnabled) {
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
        if (config != null && config.debug != null && config.debug.isDebugModeEnabled) {
           EliteMobsLogger.debug(
                   HytaleLogger.forEnclosingClass(),
                   "[Nameplate] reconcile nameplatesEnabled=%s perTier=%s role=%s tier=%d",
                   EliteMobsLogLevel.INFO,
                   String.valueOf(config.nameplates.nameplatesEnabled),
                   Arrays.toString(config.nameplates.nameplatesEnabledPerTier),
                   String.valueOf(roleName),
                   tierComponent.tierIndex
            );
        }
        if (roleName == null || roleName.isBlank()) {
            if (config != null && !config.nameplates.nameplatesEnabled) {
                plugin.getNameplateService().applyOrUpdateNameplate(config, npcRef, entityStore, commandBuffer, "", tierComponent.tierIndex);
            }
            return;
        }
        plugin.getNameplateService().applyOrUpdateNameplate(config, npcRef, entityStore, commandBuffer, roleName, tierComponent.tierIndex);
    }
}
