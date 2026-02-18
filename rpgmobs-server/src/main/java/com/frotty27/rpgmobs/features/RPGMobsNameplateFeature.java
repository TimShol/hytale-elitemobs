package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
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
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        applyNameplate(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
    }

    @Override
    public void reconcile(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                          Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                          RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        applyNameplate(plugin, config, npcRef, entityStore, commandBuffer, tierComponent, roleName);
    }

    private void applyNameplate(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                                Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                                RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        if (roleName == null || roleName.isBlank()) {
            if (config != null && config.nameplatesConfig.enableMobNameplates) {
                plugin.getNameplateService().applyOrUpdateNameplate(config,
                                                                    npcRef,
                                                                    entityStore,
                                                                    commandBuffer,
                                                                    "",
                                                                    tierComponent.tierIndex
                );
            }
            return;
        }
        plugin.getNameplateService().applyOrUpdateNameplate(config,
                                                            npcRef,
                                                            entityStore,
                                                            commandBuffer,
                                                            roleName,
                                                            tierComponent.tierIndex
        );
    }
}
