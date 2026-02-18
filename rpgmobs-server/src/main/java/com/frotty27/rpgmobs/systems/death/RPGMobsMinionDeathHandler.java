package com.frotty27.rpgmobs.systems.death;

import com.frotty27.rpgmobs.api.events.RPGMobsDeathEvent;
import com.frotty27.rpgmobs.components.RPGMobsTierComponent;
import com.frotty27.rpgmobs.components.combat.RPGMobsCombatTrackingComponent;
import com.frotty27.rpgmobs.components.summon.RPGMobsSummonedMinionComponent;
import com.frotty27.rpgmobs.plugin.RPGMobsPlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import static com.frotty27.rpgmobs.utils.ClampingHelpers.clampTierIndex;
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

        fireMinionDeathEvent(ref, npc, store);

        return true;
    }

    private void fireMinionDeathEvent(Ref<EntityStore> ref, NPCEntity npc, Store<EntityStore> store) {
        RPGMobsPlugin plugin = system.getPlugin();

        RPGMobsTierComponent tier = store.getComponent(ref, plugin.getRPGMobsComponentType());
        if (tier == null || tier.tierIndex < 0) return;

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        int tierId = clampTierIndex(tier.tierIndex);
        String roleName = npc.getRoleName() != null ? npc.getRoleName() : "";

        RPGMobsCombatTrackingComponent combatTracking = store.getComponent(ref,
                                                                           plugin.getCombatTrackingComponentType()
        );
        Ref<EntityStore> killerRef = (combatTracking != null) ? combatTracking.getBestTarget() : null;
        if (killerRef != null && !killerRef.isValid()) killerRef = null;

        plugin.getEventBus().fire(new RPGMobsDeathEvent(npc.getWorld(),
                                                         ref,
                                                         tierId,
                                                         roleName,
                                                         killerRef,
                                                         transform.getPosition().clone(),
                                                         true
        ));
    }
}
