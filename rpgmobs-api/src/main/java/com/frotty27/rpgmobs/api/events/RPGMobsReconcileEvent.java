package com.frotty27.rpgmobs.api.events;

import org.jspecify.annotations.Nullable;

/**
 * Fired when RPGMobs configuration is reloaded and all loaded entities are reconciled
 * with the new settings.
 *
 * <p>If the reconcile was scoped to a specific world, {@link #getWorldName()} returns
 * that world's name. For a global reconcile (all worlds), it returns {@code null}.</p>
 *
 * @since 1.0.0
 */
public final class RPGMobsReconcileEvent {

    private final @Nullable String worldName;
    private final int entityCount;

    /**
     * Creates a reconcile event with world scope and entity count.
     *
     * @param worldName   the name of the reconciled world, or {@code null} for a global reconcile
     * @param entityCount the number of entities that were reconciled
     * @since 1.2.0
     */
    public RPGMobsReconcileEvent(@Nullable String worldName, int entityCount) {
        this.worldName = worldName;
        this.entityCount = entityCount;
    }

    /**
     * Creates a global reconcile event with no world scope and zero entity count.
     */
    public RPGMobsReconcileEvent() {
        this(null, 0);
    }

    /**
     * Returns the name of the world that was reconciled, or {@code null} if this
     * was a global reconcile across all worlds.
     *
     * @return the world name, or {@code null}
     * @since 1.2.0
     */
    public @Nullable String getWorldName() {
        return worldName;
    }

    /**
     * Returns the number of entities that were reconciled.
     *
     * @return the entity count
     * @since 1.2.0
     */
    public int getEntityCount() {
        return entityCount;
    }
}
