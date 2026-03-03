package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

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

        NPCEntity npcEntity = entityStore.getComponent(npcRef, NPC_COMPONENT_TYPE);
        String worldName = (npcEntity != null && npcEntity.getWorld() != null) ? npcEntity.getWorld().getName() : null;
        ResolvedConfig resolved = plugin.getResolvedConfig(worldName);

        String role = (roleName != null && !roleName.isBlank()) ? roleName : "";
        plugin.getNameplateService().applyOrUpdateNameplate(config,
                                                            resolved,
                                                            npcRef,
                                                            entityStore,
                                                            commandBuffer,
                                                            role,
                                                            tierComponent.tierIndex
        );
    }
}
