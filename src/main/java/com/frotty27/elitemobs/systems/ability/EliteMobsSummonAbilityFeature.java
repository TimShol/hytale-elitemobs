package com.frotty27.elitemobs.systems.ability;

import com.frotty27.elitemobs.components.EliteMobsTierComponent;
import com.frotty27.elitemobs.config.EliteMobsConfig;
import com.frotty27.elitemobs.plugin.EliteMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

final class EliteMobsSummonAbilityFeature implements EliteMobsAbilityFeature {

    @Override
    public String id() {
        return "undead_summon";
    }

    @Override
    public void registerSystems(EliteMobsPlugin plugin) {
        plugin.registerSystem(new EliteMobsSummonAbilitySystem(plugin));
    }

    @Override
    public void onDamage(
            EliteMobsPlugin plugin,
            EliteMobsConfig config,
            Ref<EntityStore> victimRef,
            Store<EntityStore> entityStore,
            CommandBuffer<EntityStore> commandBuffer,
            EliteMobsTierComponent tierComponent,
            @Nullable NPCEntity npcEntity,
            int tierIndex,
            long currentTick,
            Damage damage
    ) {
        // no-op
    }
}
