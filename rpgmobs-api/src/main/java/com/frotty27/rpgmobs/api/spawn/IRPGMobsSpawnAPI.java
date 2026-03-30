package com.frotty27.rpgmobs.api.spawn;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

/**
 * Programmatic spawn API for creating RPGMobs elites from other mods.
 *
 * <p>Obtain an instance via {@link com.frotty27.rpgmobs.api.RPGMobsAPI#spawn()}.
 * All methods <strong>must be called on the target world's thread</strong>. If you
 * are calling from a different thread, wrap the call in
 * {@code world.execute(() -> ...)}.</p>
 *
 * <h3>Example: spawn an elite</h3>
 * <pre>{@code
 * SpawnResult result = RPGMobsAPI.spawn().spawnElite(
 *     world, "Skeleton_Fighter", 3, position, null, null);
 * if (result instanceof SpawnResult.Success s) {
 *     // s.entityRef() is the spawned elite
 * }
 * }</pre>
 *
 * <h3>Example: from another thread</h3>
 * <pre>{@code
 * world.execute(() -> {
 *     SpawnResult result = RPGMobsAPI.spawn().spawnElite(
 *         world, "Trork_Warrior", 5, pos, null, "Axes");
 * });
 * }</pre>
 *
 * <h3>Example: elite-ify an existing NPC</h3>
 * <pre>{@code
 * // You already spawned an NPC via NPCPlugin:
 * Ref<EntityStore> npcRef = ...;
 * SpawnResult result = RPGMobsAPI.spawn().applyEliteTier(world, npcRef, 4, null);
 * }</pre>
 *
 * @since 1.3.0
 */
public interface IRPGMobsSpawnAPI {

    /**
     * Spawns an NPC and promotes it to an RPGMobs elite in one call.
     *
     * <p>Creates the NPC entity via Hytale's spawn API, then applies the elite
     * tier with equipment, scaling, abilities, and CAE combat AI. Fires a
     * cancellable {@code RPGMobsSpawnedEvent} — if a listener cancels it,
     * the NPC is still spawned but will not be an elite.</p>
     *
     * <p><strong>Must be called on the world's thread.</strong></p>
     *
     * @param world     the world to spawn in
     * @param roleName  NPC role name (e.g. {@code "Skeleton_Fighter"}, {@code "Trork_Warrior"})
     * @param tier      tier number 1-5 (clamped if out of range)
     * @param position  spawn position in world coordinates
     * @param rotation  spawn rotation, or {@code null} for default
     * @param weaponCategory weapon category override (e.g. {@code "Swords"}, {@code "Axes"}),
     *                       or {@code null} to use the mob rule's default
     * @return {@link SpawnResult.Success} with the entity reference, or
     *         {@link SpawnResult.Failure} with the reason
     */
    SpawnResult spawnElite(World world, String roleName, int tier,
                           Vector3d position, @Nullable Vector3f rotation,
                           @Nullable String weaponCategory);

    /**
     * Promotes an already-spawned NPC to an RPGMobs elite.
     *
     * <p>Use this if you need custom spawn logic (specific rotation, animation,
     * or NPC variant) and want to handle the NPC creation yourself. The entity
     * must already have an {@code NPCEntity} component.</p>
     *
     * <p>Fires a cancellable {@code RPGMobsSpawnedEvent}.</p>
     *
     * <p><strong>Must be called on the world's thread.</strong></p>
     *
     * @param world   the world the NPC is in
     * @param npcRef  reference to the existing NPC entity
     * @param tier    tier number 1-5 (clamped if out of range)
     * @param weaponCategory weapon category override, or {@code null} for default
     * @return {@link SpawnResult.Success} with the entity reference, or
     *         {@link SpawnResult.Failure} with the reason
     */
    SpawnResult applyEliteTier(World world, Ref<EntityStore> npcRef,
                               int tier, @Nullable String weaponCategory);
}
