package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

/**
 * Fired after an elite dies, before loot items are spawned into the world.
 *
 * <p>The drop list is mutable - listeners can add, remove, or replace items.
 * Cancelling this event prevents all drops from spawning.</p>
 *
 * @since 1.0.0
 */
public final class RPGMobsDropsEvent extends RPGMobsEvent implements ICancellable {

    private final List<ItemStack> drops;
    private final Vector3d position;
    private boolean cancelled;

    /**
     * @param world     the world where the elite died
     * @param entityRef reference to the dead elite entity
     * @param tier      tier index (0-based)
     * @param roleName  the NPC role name
     * @param drops     mutable list of item drops - listeners may modify this list
     * @param position  the position where drops will spawn
     */
    public RPGMobsDropsEvent(World world, Ref<EntityStore> entityRef, int tier, String roleName, List<ItemStack> drops,
                             Vector3d position) {
        super(world, entityRef, tier, roleName);
        this.drops = drops;
        this.position = position;
    }

    /**
     * @return mutable list of items to drop - add, remove, or replace entries as needed
     */
    public List<ItemStack> getDrops() {
        return drops;
    }

    /**
     * @return the position where drops will be spawned
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
