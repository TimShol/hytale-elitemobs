package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Event fired when an RPG mob is spawned into the world.
 *
 * <p>This event implements {@link ICancellable}. Cancelling it will prevent the
 * RPG mob from completing its spawn initialization.</p>
 *
 * @since 1.1.0
 */
public final class RPGMobsSpawnedEvent extends RPGMobsEvent implements ICancellable {

    private final Vector3d position;
    private boolean cancelled;

    /**
     * Constructs a new spawned event.
     *
     * @param world     the world in which the RPG mob spawned
     * @param entityRef the entity reference of the spawned RPG mob
     * @param tier      the tier index of the RPG mob
     * @param roleName  the role name of the RPG mob
     * @param position  the world position where the RPG mob spawned
     */
    public RPGMobsSpawnedEvent(World world, Ref<EntityStore> entityRef, int tier, String roleName, Vector3d position) {
        super(world, entityRef, tier, roleName);
        this.position = position;
    }

    /**
     * Returns the world position where the RPG mob spawned.
     *
     * @return the spawn position, never {@code null}
     */
    public Vector3d getPosition() {
        return position;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
