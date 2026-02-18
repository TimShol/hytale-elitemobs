package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an RPG mob loses its aggro target.
 *
 * <p>This occurs when the RPG mob is no longer actively targeting an entity,
 * for example when the target moves out of range, dies, or disconnects.
 * Listeners can use this event to clean up combat-related state.</p>
 *
 * @since 1.1.0
 */
public final class RPGMobsDeaggroEvent extends RPGMobsEvent {

    /**
     * Constructs a new deaggro event.
     *
     * @param world    the world in which the deaggro occurred
     * @param mobRef   the entity reference of the RPG mob that lost its target
     * @param tier     the tier index of the RPG mob
     * @param roleName the role name of the RPG mob
     */
    public RPGMobsDeaggroEvent(World world, Ref<EntityStore> mobRef, int tier, String roleName) {
        super(world, mobRef, tier, roleName);
    }
}
