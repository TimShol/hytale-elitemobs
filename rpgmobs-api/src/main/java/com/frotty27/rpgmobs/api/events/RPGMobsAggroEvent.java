package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an RPG mob acquires an aggro target.
 *
 * <p>This event is triggered when the RPG mob begins targeting a specific entity,
 * typically a player. It can be used to react to combat initiation or to trigger
 * custom behaviors when an RPG mob locks onto a target.</p>
 *
 * @since 1.1.0
 */
public final class RPGMobsAggroEvent extends RPGMobsEvent {

    private final Ref<EntityStore> targetRef;

    /**
     * Constructs a new aggro event.
     *
     * @param mobRef    the entity reference of the RPG mob that acquired a target
     * @param targetRef the entity reference of the entity being targeted
     * @param tier      the tier index of the RPG mob
     * @param roleName  the role name of the RPG mob
     */
    public RPGMobsAggroEvent(Ref<EntityStore> mobRef, Ref<EntityStore> targetRef, int tier, String roleName) {
        super(mobRef, tier, roleName);
        this.targetRef = targetRef;
    }

    /**
     * Returns the entity reference of the entity being targeted by the RPG mob.
     *
     * @return the target's entity reference, never {@code null}
     */
    public Ref<EntityStore> targetRef() {
        return targetRef;
    }
}
