package com.frotty27.rpgmobs.api.spawn;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Result of a programmatic elite spawn or tier application.
 *
 * <p>Pattern-match on {@link Success} or {@link Failure} to handle the outcome:</p>
 * <pre>{@code
 * SpawnResult result = RPGMobsAPI.spawn().spawnElite(world, "Skeleton_Fighter", 3, pos, null, null);
 * if (result instanceof SpawnResult.Success s) {
 *     Ref<EntityStore> ref = s.entityRef();
 *     // use the spawned elite
 * } else if (result instanceof SpawnResult.Failure f) {
 *     logger.warn("Spawn failed: " + f.reason() + " - " + f.message());
 * }
 * }</pre>
 *
 * @since 1.3.0
 */
public sealed interface SpawnResult permits SpawnResult.Success, SpawnResult.Failure {

    /**
     * The spawn or tier application succeeded.
     *
     * @param entityRef reference to the spawned/promoted elite entity
     * @param tier      tier index (0 = T1, 4 = T5)
     * @param roleName  the NPC role name that was spawned
     */
    record Success(Ref<EntityStore> entityRef, int tier, String roleName) implements SpawnResult {
    }

    /**
     * The spawn or tier application failed.
     *
     * @param reason  categorized failure reason
     * @param message human-readable description of what went wrong
     */
    record Failure(Reason reason, String message) implements SpawnResult {
    }

    /**
     * Categorized failure reasons for spawn operations.
     */
    enum Reason {
        /** RPGMobs plugin has not finished initializing. */
        NOT_INITIALIZED,
        /** RPGMobs config is not loaded yet. */
        CONFIG_NOT_LOADED,
        /** Hytale's NPC spawn API failed to create the entity. */
        NPC_SPAWN_FAILED,
        /** No mob rule matched the given role name. */
        NO_MOB_RULE,
        /** The matched mob rule is disabled in this world's overlay. */
        MOB_RULE_DISABLED,
        /** RPGMobs is disabled in the target world. */
        RPGMOBS_DISABLED_IN_WORLD,
        /** A listener cancelled the {@code RPGMobsSpawnedEvent}. */
        EVENT_CANCELLED,
        /** Tier application failed for an internal reason. */
        TIER_APPLY_FAILED
    }

    /**
     * Returns {@code true} if this result is a {@link Success}.
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }
}
