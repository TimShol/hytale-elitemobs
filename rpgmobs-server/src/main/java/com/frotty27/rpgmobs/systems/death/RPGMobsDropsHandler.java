package com.frotty27.rpgmobs.systems.death;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

final class RPGMobsDropsHandler {

    private final RPGMobsDeathSystem system;

    RPGMobsDropsHandler(RPGMobsDeathSystem system) {
        this.system = system;
    }

    void handle(@NonNull Ref<EntityStore> ref, @NonNull DeathComponent death, @NonNull Store<EntityStore> store) {
        system.processOnDeath(ref, death, store);
    }
}
