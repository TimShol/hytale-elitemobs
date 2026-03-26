package com.frotty27.rpgmobs.features;

import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.config.RPGMobsConfig;
import com.frotty27.rpgmobs.config.overlay.ResolvedConfig;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.frotty27.rpgmobs.systems.combat.RPGMobsDamageDealtSystem;
import com.frotty27.rpgmobs.systems.death.RPGMobsDeathSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public final class RPGMobsDamageFeature implements IRPGMobsFeature {

    @Override
    public String getFeatureKey() {
        return "Damage";
    }

    @Override
    public Object getConfig(RPGMobsConfig config) {
        return config.damageConfig;
    }

    @Override
    public void apply(RPGMobsPlugin plugin, RPGMobsConfig config, ResolvedConfig resolved,
                      Ref<EntityStore> npcRef, Store<EntityStore> entityStore,
                      CommandBuffer<EntityStore> commandBuffer, RPGMobsTierComponent tierComponent,
                      @Nullable String roleName) {
        RPGMobsCombatTrackingComponent combatTracking = new RPGMobsCombatTrackingComponent();
        commandBuffer.putComponent(npcRef, plugin.getCombatTrackingComponentType(), combatTracking);
    }

    @Override
    public void cleanup(RPGMobsPlugin plugin, RPGMobsConfig config, Ref<EntityStore> npcRef,
                        Store<EntityStore> entityStore, CommandBuffer<EntityStore> commandBuffer) {
        commandBuffer.tryRemoveComponent(npcRef, plugin.getCombatTrackingComponentType());
    }

    @Override
    public void registerSystems(RPGMobsPlugin plugin) {
        plugin.registerSystem(new RPGMobsDeathSystem(plugin));
        plugin.registerSystem(new RPGMobsDamageDealtSystem(plugin));
    }
}
