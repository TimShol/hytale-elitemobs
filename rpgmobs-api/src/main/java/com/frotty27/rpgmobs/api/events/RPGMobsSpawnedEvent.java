package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Fired when an NPC is promoted to an RPGMobs elite.
 *
 * <p>This event is cancellable. Setting {@code setCancelled(true)} prevents
 * the NPC from becoming an elite - it remains a normal NPC.</p>
 *
 * @since 1.0.0
 */
public final class RPGMobsSpawnedEvent extends RPGMobsEvent implements ICancellable {

    private final Vector3d position;
    private boolean cancelled;

    /**
     * @param world     the world in which the elite spawned
     * @param entityRef reference to the NPC entity
     * @param tier      tier index (0-based, 0 = T1 through 4 = T5)
     * @param roleName  the NPC role name (e.g. "Skeleton_Warrior")
     * @param position  the spawn position
     */
    public RPGMobsSpawnedEvent(World world, Ref<EntityStore> entityRef, int tier, String roleName, Vector3d position) {
        super(world, entityRef, tier, roleName);
        this.position = position;
    }

    /**
     * @return the position where the elite spawned
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
