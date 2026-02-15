package com.frotty27.elitemobs.systems.death;

import java.util.Objects;

import com.frotty27.elitemobs.components.summon.EliteMobsSummonedMinionComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

final class EliteMobsMinionDeathHandler {

    private final EliteMobsDeathSystem system;

    EliteMobsMinionDeathHandler(EliteMobsDeathSystem system) {
        this.system = system;
    }

    boolean handle(
            @NonNull Ref<EntityStore> ref,
            @NonNull DeathComponent death,
            @NonNull Store<EntityStore> store,
            @NonNull CommandBuffer<EntityStore> cb
    ) {
        NPCEntity npc = store.getComponent(ref, Objects.requireNonNull(NPCEntity.getComponentType()));
        if (npc == null) return false;

        EliteMobsSummonedMinionComponent minion =
                store.getComponent(ref, system.getSummonedMinionComponentType());
        if (minion == null) return false;

        system.decrementSummonerAliveCount(npc, minion, store);
        return true;
    }
}
