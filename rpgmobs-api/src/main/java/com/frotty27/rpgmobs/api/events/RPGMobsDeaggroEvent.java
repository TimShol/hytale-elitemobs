package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired when an RPGMobs elite loses its combat target and exits combat.
 *
 * @since 1.0.0
 */
public final class RPGMobsDeaggroEvent extends RPGMobsEvent {

    /**
     * @param world    the world where the elite exited combat
     * @param mobRef   reference to the elite entity
     * @param tier     tier index (0-based)
     * @param roleName the NPC role name
     */
    public RPGMobsDeaggroEvent(World world, Ref<EntityStore> mobRef, int tier, String roleName) {
        super(world, mobRef, tier, roleName);
    }
}
