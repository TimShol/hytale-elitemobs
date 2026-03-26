package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired when an RPGMobs elite acquires a combat target and enters combat. The entityRef
 * from the parent class is the elite that aggroed.
 *
 * @since 1.0.0
 */
public final class RPGMobsAggroEvent extends RPGMobsEvent {

    private final Ref<EntityStore> targetRef;

    /**
     * @param world     the world where the elite entered combat
     * @param mobRef    reference to the elite entity
     * @param targetRef reference to the entity being targeted
     * @param tier      tier index (0-based)
     * @param roleName  the NPC role name
     */
    public RPGMobsAggroEvent(World world, Ref<EntityStore> mobRef, Ref<EntityStore> targetRef, int tier,
                             String roleName) {
        super(world, mobRef, tier, roleName);
        this.targetRef = targetRef;
    }

    /**
     * Returns the entity that the elite has targeted.
     *
     * @return the target entity reference
     */
    public Ref<EntityStore> getTargetRef() {
        return targetRef;
    }
}
