package com.frotty27.rpgmobs.api;

import com.frotty27.rpgmobs.api.events.*;

/**
 * Listener interface for receiving RPGMobs events.
 *
 * <p>Implement this interface and override the event handlers you are interested in.
 * All handler methods have default no-op implementations, so you only need to override
 * the ones relevant to your mod.</p>
 *
 * <p>Register your listener via {@link RPGMobsAPI#registerListener(IRPGMobsEventListener)}.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * public class MyListener implements IRPGMobsEventListener {
 *     @Override
 *     public void onRPGMobDeath(RPGMobsDeathEvent event) {
 *         // React to RPG mob deaths
 *     }
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public interface IRPGMobsEventListener {

    /**
     * Called when an RPG mob is spawned into the world.
     *
     * @param event the spawn event; may be cancelled to prevent the spawn
     */
    default void onRPGMobSpawned(RPGMobsSpawnedEvent event) {
    }

    /**
     * Called when an RPG mob dies.
     *
     * @param event the death event containing the killer reference and death position
     */
    default void onRPGMobDeath(RPGMobsDeathEvent event) {
    }

    /**
     * Called when an RPG mob's drops are about to be spawned.
     *
     * <p>Listeners may modify the drop list or cancel the event to suppress drops entirely.</p>
     *
     * @param event the drops event; may be cancelled to suppress all drops
     */
    default void onRPGMobDrops(RPGMobsDropsEvent event) {
    }

    /**
     * Called when an RPG mob deals damage to another entity.
     *
     * <p>Listeners may modify the damage multiplier or cancel the event.</p>
     *
     * @param event the damage dealt event; may be cancelled to prevent the damage
     */
    default void onRPGMobDamageDealt(RPGMobsDamageDealtEvent event) {
    }

    /**
     * Called when an RPG mob receives damage from another entity or source.
     *
     * @param event the damage received event
     */
    default void onRPGMobDamageReceived(RPGMobsDamageReceivedEvent event) {
    }

    /**
     * Called during a reconciliation pass, allowing listeners to synchronize
     * their state with the current RPG mob data.
     *
     * @param event the reconcile event
     */
    default void onReconcile(RPGMobsReconcileEvent event) {
    }

    /**
     * Called when an RPG mob begins executing an ability.
     *
     * <p>This event may be cancelled to prevent the ability from starting.</p>
     *
     * @param event the ability started event; may be cancelled
     */
    default void onRPGMobAbilityStarted(RPGMobsAbilityStartedEvent event) {
    }

    /**
     * Called when an RPG mob successfully completes an ability.
     *
     * @param event the ability completed event
     */
    default void onRPGMobAbilityCompleted(RPGMobsAbilityCompletedEvent event) {
    }

    /**
     * Called when an RPG mob's ability is interrupted before completion.
     *
     * @param event the ability interrupted event, including the interruption reason
     */
    default void onRPGMobAbilityInterrupted(RPGMobsAbilityInterruptedEvent event) {
    }

    /**
     * Called after health, damage, and model scaling have been applied to an RPG mob.
     *
     * @param event the scaling applied event containing all computed multipliers
     */
    default void onScalingApplied(RPGMobsScalingAppliedEvent event) {
    }

    /**
     * Called when an RPG mob acquires an aggro target.
     *
     * @param event the aggro event containing the target reference
     */
    default void onRPGMobAggro(RPGMobsAggroEvent event) {
    }

    /**
     * Called when an RPG mob loses its aggro target.
     *
     * @param event the deaggro event
     */
    default void onRPGMobDeaggro(RPGMobsDeaggroEvent event) {
    }
}
