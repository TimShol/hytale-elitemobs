package com.frotty27.rpgmobs.api.query;

import com.frotty27.rpgmobs.api.RPGMobsAPI;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Optional;
import java.util.Set;

/**
 * Read-only query interface for inspecting the state of RPG Mobs.
 *
 * <p>Obtain an instance via {@link RPGMobsAPI#query()}.
 * All methods accept an entity reference and return the requested data if the
 * entity is a recognized RPG mob, or {@link Optional#empty()} / default values
 * otherwise.</p>
 *
 * @since 1.1.0
 */
public interface IRPGMobsQueryAPI {

    /**
     * Returns the tier index of the specified entity, if it is an RPG mob.
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the zero-based tier index, or empty
     * if the entity is not an RPG mob
     */
    Optional<Integer> getTier(Ref<EntityStore> entityRef);

    /**
     * Returns whether the specified entity is an RPG mob.
     *
     * @param entityRef the entity reference to query
     * @return {@code true} if the entity is an RPG mob, {@code false} otherwise
     */
    boolean isRPGMob(Ref<EntityStore> entityRef);

    /**
     * Returns whether the specified entity is a summoned minion of an RPG mob.
     *
     * <p>A minion is a mob that was spawned by another RPG mob's summon ability,
     * rather than through the normal spawn system.</p>
     *
     * @param entityRef the entity reference to query
     * @return {@code true} if the entity is a summoned minion, {@code false} otherwise
     * @since 2.0.2
     */
    boolean isMinion(Ref<EntityStore> entityRef);

    /**
     * Returns the distance-based health bonus applied to the specified RPG mob.
     *
     * <p>This bonus is determined by how far the mob spawned from the world origin,
     * increasing difficulty in more remote areas.</p>
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the health bonus, or empty if the
     * entity is not an RPG mob or has no distance scaling applied
     */
    Optional<Float> getDistanceHealthBonus(Ref<EntityStore> entityRef);

    /**
     * Returns the distance-based damage bonus applied to the specified RPG mob.
     *
     * <p>This bonus is determined by how far the mob spawned from the world origin,
     * increasing difficulty in more remote areas.</p>
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the damage bonus, or empty if the
     * entity is not an RPG mob or has no distance scaling applied
     */
    Optional<Float> getDistanceDamageBonus(Ref<EntityStore> entityRef);

    /**
     * Returns the distance from the world origin at which the specified RPG mob spawned.
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the spawn distance, or empty if the
     * entity is not an RPG mob or the distance is not tracked
     */
    Optional<Float> getSpawnDistance(Ref<EntityStore> entityRef);

    /**
     * Returns the health multiplier applied to the specified RPG mob.
     *
     * <p>This multiplier scales the mob's base health based on tier and
     * distance-based bonuses.</p>
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the health multiplier, or empty if
     * the entity is not an RPG mob or health scaling is not enabled
     */
    Optional<Float> getHealthMultiplier(Ref<EntityStore> entityRef);

    /**
     * Returns the damage multiplier applied to the specified RPG mob.
     *
     * <p>This multiplier scales the mob's outgoing damage based on tier and
     * distance-based bonuses.</p>
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the damage multiplier, or empty if
     * the entity is not an RPG mob or damage scaling is not enabled
     */
    Optional<Float> getDamageMultiplier(Ref<EntityStore> entityRef);

    /**
     * Returns the model scale factor applied to the specified RPG mob.
     *
     * <p>Higher-tier mobs may be visually larger to communicate their difficulty.</p>
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the model scale, or empty if the
     * entity is not an RPG mob or model scaling is not enabled
     */
    Optional<Float> getModelScale(Ref<EntityStore> entityRef);

    /**
     * Returns whether the health of the specified RPG mob has been finalized.
     *
     * <p>Health finalization occurs after all scaling calculations are complete
     * and the final health value has been applied to the entity.</p>
     *
     * @param entityRef the entity reference to query
     * @return {@code true} if health has been finalized, {@code false} if the entity
     * is not an RPG mob or health has not yet been finalized
     */
    boolean isHealthFinalized(Ref<EntityStore> entityRef);

    /**
     * Returns the number of summoned minions currently active for the specified RPG mob.
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the minion count, or empty if the
     * entity is not an RPG mob or does not track summoned minions
     */
    Optional<Integer> getSummonedMinionCount(Ref<EntityStore> entityRef);

    /**
     * Returns the entity reference of the last aggro target for the specified RPG mob.
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the last aggro target reference, or
     * empty if the entity is not an RPG mob or has no recorded aggro target
     */
    Optional<Ref<EntityStore>> getLastAggroTarget(Ref<EntityStore> entityRef);

    /**
     * Returns the server tick at which the specified RPG mob last acquired an aggro target.
     *
     * @param entityRef the entity reference to query
     * @return an {@link Optional} containing the tick number, or empty if the entity
     * is not an RPG mob or has no recorded aggro event
     */
    Optional<Long> getLastAggroTick(Ref<EntityStore> entityRef);

    /**
     * Returns whether the specified RPG mob is currently in combat.
     *
     * <p>an RPG mob is considered in combat when it has an active aggro target.</p>
     *
     * @param entityRef the entity reference to query
     * @return {@code true} if the RPG mob is in combat, {@code false} otherwise
     */
    boolean isInCombat(Ref<EntityStore> entityRef);

    /**
     * Returns the migration version of the specified RPG mob's component data.
     *
     * <p>Used internally to track which data migrations have been applied to the entity.</p>
     *
     * @param entityRef the entity reference to query
     * @return the migration version number, or {@code 0} if the entity is not an RPG mob
     */
    int getMigrationVersion(Ref<EntityStore> entityRef);

    /**
     * Returns whether the specified RPG mob needs a data migration.
     *
     * <p>an RPG mob needs migration if its stored migration version is lower than
     * the current expected version.</p>
     *
     * @param entityRef the entity reference to query
     * @return {@code true} if migration is needed, {@code false} otherwise
     */
    boolean needsMigration(Ref<EntityStore> entityRef);

    /**
     * Returns the set of all supported ability trigger type identifiers.
     *
     * @return an unmodifiable set of trigger type strings, never {@code null}
     */
    Set<String> getSupportedTriggerTypes();

    /**
     * Returns whether the specified trigger type is supported by the ability system.
     *
     * @param triggerType the trigger type identifier to check
     * @return {@code true} if the trigger type is supported, {@code false} otherwise
     */
    boolean isTriggerTypeSupported(String triggerType);
}
