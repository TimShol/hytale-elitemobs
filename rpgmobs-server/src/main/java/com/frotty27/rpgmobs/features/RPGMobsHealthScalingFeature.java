package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsHealthScalingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.visual.HealthScalingSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public final class RPGMobsHealthScalingFeature implements IRPGMobsFeature {

    @Override
    public String getFeatureKey() {
        return "HealthScaling";
    }

    @Override
    public Object getConfig(RPGMobsConfig config) {
        return config.healthConfig;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        if (config.healthConfig.enableMobHealthScaling) {
            RPGMobsHealthScalingComponent healthScaling = new RPGMobsHealthScalingComponent();
            commandBuffer.putComponent(npcRef, plugin.getHealthScalingComponentType(), healthScaling);
        }
    }

    @Override
    public void reconcile(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                          Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                          RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        RPGMobsHealthScalingComponent healthComp = entityStore.getComponent(npcRef,
                plugin.getHealthScalingComponentType());

        NPCEntity npc = entityStore.getComponent(npcRef, NPC_COMPONENT_TYPE);
        String worldName = (npc != null && npc.getWorld() != null) ? npc.getWorld().getName() : null;
        ResolvedConfig resolved = plugin.getResolvedConfig(worldName);

        if (!resolved.enableHealthScaling) {
            if (healthComp != null && healthComp.healthApplied) {
                EntityStatMap entityStats = entityStore.getComponent(npcRef, EntityStatMap.getComponentType());
                if (entityStats != null) {
                    int healthStatId = DefaultEntityStatTypes.getHealth();
                    entityStats.removeModifier(healthStatId, getFeatureKey());
                    entityStats.maximizeStatValue(healthStatId);
                    commandBuffer.replaceComponent(npcRef, EntityStatMap.getComponentType(), entityStats);
                }
                healthComp.healthApplied = false;
                healthComp.healthFinalized = false;
                commandBuffer.replaceComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);
            }
            return;
        }

        if (healthComp == null) {
            healthComp = new RPGMobsHealthScalingComponent();
            commandBuffer.putComponent(npcRef, plugin.getHealthScalingComponentType(), healthComp);
        }
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
        HealthScalingSystem system = new HealthScalingSystem(plugin, this);
        plugin.registerSystem(system);
    }
}
