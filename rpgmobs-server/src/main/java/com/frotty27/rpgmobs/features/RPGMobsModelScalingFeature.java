package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.lifecycle.RPGMobsModelScalingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.visual.ModelScalingSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

public final class RPGMobsModelScalingFeature implements IRPGMobsFeature {

    private ModelScalingSystem modelScalingSystem;

    @Override
    public String getFeatureKey() {
        return "ModelScaling";
    }

    @Override
    public Object getConfig(RPGMobsConfig config) {
        return config.modelConfig;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                      Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                      RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        if (config.modelConfig.enableMobModelScaling) {
            RPGMobsModelScalingComponent modelScaling = new RPGMobsModelScalingComponent();
            commandBuffer.putComponent(npcRef, plugin.getModelScalingComponentType(), modelScaling);
        }
    }

    @Override
    public void reconcile(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                          Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer,
                          RPGMobsTierComponent tierComponent, @Nullable String roleName) {
        NPCEntity npc = entityStore.getComponent(npcRef, NPC_COMPONENT_TYPE);
        String worldName = (npc != null && npc.getWorld() != null) ? npc.getWorld().getName() : null;
        ResolvedConfig resolved = plugin.getResolvedConfig(worldName);

        RPGMobsModelScalingComponent modelComp = entityStore.getComponent(npcRef,
                plugin.getModelScalingComponentType());

        if (!resolved.enableModelScaling) {
            if (modelComp != null && modelComp.scaledApplied
                    && Math.abs(modelComp.appliedScale - 1.0f) > 0.001f) {
                modelScalingSystem.resetModelScale(npcRef, entityStore, commandBuffer);
                modelComp.scaledApplied = false;
                modelComp.appliedScale = 1.0f;
                modelComp.configuredBaseScale = 1.0f;
                modelComp.resyncVerified = true;
                commandBuffer.replaceComponent(npcRef, plugin.getModelScalingComponentType(), modelComp);
            }
            return;
        }

        if (modelComp == null) {
            modelComp = new RPGMobsModelScalingComponent();
            commandBuffer.putComponent(npcRef, plugin.getModelScalingComponentType(), modelComp);
        }
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
        modelScalingSystem = new ModelScalingSystem(plugin);
        plugin.registerSystem(modelScalingSystem);
        plugin.getEventBus().registerListener(modelScalingSystem);
    }

    public ModelScalingSystem getModelScalingSystem() {
        return modelScalingSystem;
    }
}
