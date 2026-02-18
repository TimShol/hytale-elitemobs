package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

/**
 * Event fired when an RPG mob dies.
 *
 * <p>Contains information about who killed the mob (if applicable), the
 * position of death, and whether the mob was a summoned minion.</p>
 *
 * @since 1.1.0
 */
public final class RPGMobsDeathEvent extends RPGMobsEvent {

    private final @Nullable Ref<EntityStore> killerRef;
    private final Vector3d position;
    private final boolean minion;

    /**
     * Constructs a new death event.
     *
     * @param world     the world in which the RPG mob died
     * @param entityRef the entity reference of the RPG mob that died
     * @param tier      the tier index of the RPG mob
     * @param roleName  the role name of the RPG mob
     * @param killerRef the entity reference of the killer, or {@code null} if the death
     *                  was not caused by another entity (e.g., environmental damage)
     * @param position  the world position where the RPG mob died
     * @param minion    whether the dead RPG mob was a summoned minion
     */
    public RPGMobsDeathEvent(World world, Ref<EntityStore> entityRef, int tier, String roleName,
                             @Nullable Ref<EntityStore> killerRef, Vector3d position, boolean minion) {
        super(world, entityRef, tier, roleName);
        this.killerRef = killerRef;
        this.position = position;
        this.minion = minion;
    }

    /**
     * Constructs a new death event for a regular (non-minion) RPG mob.
     *
     * @param world     the world in which the RPG mob died
     * @param entityRef the entity reference of the RPG mob that died
     * @param tier      the tier index of the RPG mob
     * @param roleName  the role name of the RPG mob
     * @param killerRef the entity reference of the killer, or {@code null} if the death
     *                  was not caused by another entity
     * @param position  the world position where the RPG mob died
     */
    public RPGMobsDeathEvent(World world, Ref<EntityStore> entityRef, int tier, String roleName,
                             @Nullable Ref<EntityStore> killerRef, Vector3d position) {
        this(world, entityRef, tier, roleName, killerRef, position, false);
    }

    /**
     * Returns the entity reference of the killer, if any.
     *
     * @return the killer's entity reference, or {@code null} if the death was not
     * caused by another entity
     */
    public @Nullable Ref<EntityStore> getKillerRef() {
        return killerRef;
    }

    /**
     * Returns the world position where the RPG mob died.
     *
     * @return the death position, never {@code null}
     */
    public Vector3d getPosition() {
        return position;
    }

    /**
     * Returns whether the dead RPG mob was a summoned minion.
     *
     * <p>Summoned minions die when their summoner dies or when they expire.
     * Listeners that only care about regular RPG mob deaths can check this
     * flag and return early.</p>
     *
     * @return {@code true} if this death was a minion, {@code false} for regular RPG mobs
     * @since 1.2.0
     */
    public boolean isMinion() {
        return minion;
    }
}
