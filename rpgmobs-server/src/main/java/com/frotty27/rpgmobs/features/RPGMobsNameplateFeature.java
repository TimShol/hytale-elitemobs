package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.services.RPGMobsNameplateService;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsNameplateFeature implements IRPGMobsFeature {

    @Override
    public String getFeatureKey() {
        return "Nameplate";
    }

    @Override
    public Object getConfig(RPGMobsConfig config) {
        return config.nameplatesConfig;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, ResolvedConfig resolved,
                      Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                      CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent,
                      @Nullable String roleName) {
        String role = (roleName != null && !roleName.isBlank()) ? roleName : "";
        plugin.getNameplateService().applyOrUpdateNameplate(config, resolved, npcRef, entityStore,
                                                            commandBuffer, role, tierComponent.tierIndex);
    }

    @Override
    public void reconcile(RPGMobsPlugin plugin, RPGMobsConfig config, ResolvedConfig resolved,
                          Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                          CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent,
                          @Nullable String roleName) {
        String role = (roleName != null && !roleName.isBlank()) ? roleName : "";
        plugin.getNameplateService().applyOrUpdateNameplate(config, resolved, npcRef, entityStore,
                                                            commandBuffer, role, tierComponent.tierIndex);
        plugin.getNameplateService().updateDebugSegment(plugin, npcRef, entityStore,
                                                         commandBuffer, config.debugConfig.isDebugModeEnabled);
    }

    @Override
    public void cleanup(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                        Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        RPGMobsNameplateService.removeAllSegments(entityStore, npcRef);
    }
}
