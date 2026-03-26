package com.frotty27.rpgmobs.api.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

/**
 * Fired when an RPGMobs elite dies.
 *
 * <p>Provides the killer reference (null for environmental deaths),
 * the death position, and whether the deceased was a summoned minion.</p>
 *
 * @since 1.0.0
 */
public final class RPGMobsDeathEvent extends RPGMobsEvent {

    private final @Nullable Ref<EntityStore> killerRef;
    private final Vector3d position;
    private final boolean minion;

    /**
     * @param world     the world where the elite died
     * @param entityRef reference to the dead elite entity
     * @param tier      tier index (0-based)
     * @param roleName  the NPC role name
     * @param killerRef reference to the killing entity, or {@code null} for environmental deaths
     * @param position  the death position
     * @param minion    {@code true} if the deceased was a summoned minion
     */
    public RPGMobsDeathEvent(World world, Ref<EntityStore> entityRef, int tier, String roleName,
                             @Nullable Ref<EntityStore> killerRef, Vector3d position, boolean minion) {
        super(world, entityRef, tier, roleName);
        this.killerRef = killerRef;
        this.position = position;
        this.minion = minion;
    }

    /**
     * Convenience constructor that defaults {@code minion} to {@code false}.
     *
     * @param world     the world where the elite died
     * @param entityRef reference to the dead elite entity
     * @param tier      tier index (0-based)
     * @param roleName  the NPC role name
     * @param killerRef reference to the killing entity, or {@code null} for environmental deaths
     * @param position  the death position
     */
    public RPGMobsDeathEvent(World world, Ref<EntityStore> entityRef, int tier, String roleName,
                             @Nullable Ref<EntityStore> killerRef, Vector3d position) {
        this(world, entityRef, tier, roleName, killerRef, position, false);
    }

    /**
     * @return the entity that killed this elite, or {@code null} for environmental deaths
     */
    public @Nullable Ref<EntityStore> getKillerRef() {
        return killerRef;
    }

    /**
     * @return the position where the elite died
     */
    public Vector3d getPosition() {
        return position;
    }

    /**
     * @return {@code true} if the deceased was a summoned minion
     */
    public boolean isMinion() {
        return minion;
    }
}
