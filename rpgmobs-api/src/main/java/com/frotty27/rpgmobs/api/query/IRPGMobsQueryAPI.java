package com.frotty27.rpgmobs.api.query;

import com.frotty27.rpgmobs.api.RPGMobsAPI;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;

/**
 * Read-only query interface for inspecting RPGMobs entity state.
 *
 * <p>Obtain an instance via {@link RPGMobsAPI#query()}. All methods are
 * thread-safe and return {@link Optional#empty()} when the entity lacks
 * the relevant component.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * IRPGMobsQueryAPI query = RPGMobsAPI.query();
 *
 * if (query.isRPGMob(entityRef)) {
 *     int tier = query.getTier(entityRef).orElse(0);
 *     boolean fighting = query.isInCombat(entityRef);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface IRPGMobsQueryAPI {

    /**
     * Returns the tier index of the entity (0 = T1, 4 = T5).
     *
     * @param entityRef the entity to query
     * @return tier index, or empty if the entity is not an RPGMobs elite
     */
    Optional<Integer> getTier(Ref<EntityStore> entityRef);

    /**
     * Returns whether the entity is an RPGMobs elite (has a tier component).
     *
     * @param entityRef the entity to check
     * @return true if the entity is an RPGMobs elite
     */
    boolean isRPGMob(Ref<EntityStore> entityRef);

    /**
     * Returns whether the entity is a summoned minion.
     *
     * @param entityRef the entity to check
     * @return true if the entity is a summoned minion
     */
    boolean isMinion(Ref<EntityStore> entityRef);

    /**
     * Returns the distance-based health bonus applied to this elite.
     *
     * @param entityRef the entity to query
     * @return health bonus multiplier, or empty if not applicable
     */
    Optional<Float> getDistanceHealthBonus(Ref<EntityStore> entityRef);

    /**
     * Returns the distance-based damage bonus applied to this elite.
     *
     * @param entityRef the entity to query
     * @return damage bonus multiplier, or empty if not applicable
     */
    Optional<Float> getDistanceDamageBonus(Ref<EntityStore> entityRef);

    /**
     * Returns the distance from the world spawn point where this elite was created.
     *
     * @param entityRef the entity to query
     * @return spawn distance in blocks, or empty if not tracked
     */
    Optional<Float> getSpawnDistance(Ref<EntityStore> entityRef);

    /**
     * Returns the total health multiplier applied to this elite.
     *
     * @param entityRef the entity to query
     * @return health multiplier, or empty if health scaling has not been applied
     */
    Optional<Float> getHealthMultiplier(Ref<EntityStore> entityRef);

    /**
     * Returns the total damage multiplier for this elite (tier + distance bonus).
     *
     * @param entityRef the entity to query
     * @return damage multiplier, or empty if the entity is not an elite
     */
    Optional<Float> getDamageMultiplier(Ref<EntityStore> entityRef);

    /**
     * Returns the model scale factor applied to this elite.
     *
     * @param entityRef the entity to query
     * @return model scale, or empty if model scaling has not been applied
     */
    Optional<Float> getModelScale(Ref<EntityStore> entityRef);

    /**
     * Returns whether the elite's health has been finalized (fully scaled and locked).
     *
     * @param entityRef the entity to check
     * @return true if health is finalized
     */
    boolean isHealthFinalized(Ref<EntityStore> entityRef);

    /**
     * Returns the number of alive summoned minions for a summoner elite.
     *
     * @param entityRef the summoner entity to query
     * @return minion count, or empty if the entity is not a summoner
     */
    Optional<Integer> getSummonedMinionCount(Ref<EntityStore> entityRef);

    /**
     * Returns the entity reference of the elite's current combat target.
     *
     * @param entityRef the entity to query
     * @return target reference, or empty if not in combat or no target
     */
    Optional<Ref<EntityStore>> getLastAggroTarget(Ref<EntityStore> entityRef);

    /**
     * Returns the tick at which the elite's combat state last changed.
     *
     * @param entityRef the entity to query
     * @return state change tick, or empty if no combat state tracked
     */
    Optional<Long> getLastAggroTick(Ref<EntityStore> entityRef);

    /**
     * Returns whether the elite is currently in active combat.
     *
     * @param entityRef the entity to check
     * @return true if in combat
     */
    boolean isInCombat(Ref<EntityStore> entityRef);
}
