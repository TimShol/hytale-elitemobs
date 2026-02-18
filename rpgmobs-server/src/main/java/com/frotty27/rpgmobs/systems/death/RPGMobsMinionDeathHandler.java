package com.frotty27.rpgmobs.systems.death;

import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import static com.frotty27.rpgmobs.utils.Constants.NPC_COMPONENT_TYPE;

final class RPGMobsMinionDeathHandler {

    private final RPGMobsDeathSystem system;

    RPGMobsMinionDeathHandler(RPGMobsDeathSystem system) {
        this.system = system;
    }

    boolean handle(@NonNull Ref<EntityStore> ref, @NonNull DeathComponent death, @NonNull Store<EntityStore> store,
                   @NonNull CommandBuffer<EntityStore> cb) {
        NPCEntity npc = store.getComponent(ref, NPC_COMPONENT_TYPE);
        if (npc == null) return false;

        RPGMobsSummonedMinionComponent minion = store.getComponent(ref, system.getSummonedMinionComponentType());
        if (minion == null) return false;

        death.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);
        system.decrementSummonerAliveCount(npc, minion, store, cb);
        return true;
    }
}
