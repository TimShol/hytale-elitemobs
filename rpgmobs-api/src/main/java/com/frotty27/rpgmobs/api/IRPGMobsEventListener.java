package com.frotty27.rpgmobs.api;

import com.frotty27.rpgmobs.api.events.*;

/**
 * Listener interface for RPGMobs events.
 *
 * <p>Implement this interface and override only the event callbacks you need.
 * All methods have default no-op implementations, so you only handle what matters
 * to your plugin.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class MyListener implements IRPGMobsEventListener {
 *     @Override
 *     public void onRPGMobDeath(RPGMobsDeathEvent event) {
 *         // Custom death handling
 *     }
 *
 *     @Override
 *     public void onRPGMobDrops(RPGMobsDropsEvent event) {
 *         // Modify or cancel drops
 *         event.getDrops().clear();
 *     }
 * }
 * }</pre>
 *
 * @see RPGMobsAPI#registerListener(IRPGMobsEventListener)
 * @since 1.0.0
 */
public interface IRPGMobsEventListener {

    /**
     * Called when an NPC is promoted to an RPGMobs elite.
     * Cancellable - setting {@code event.setCancelled(true)} prevents the elite from spawning.
     */
    default void onRPGMobSpawned(RPGMobsSpawnedEvent event) {
    }

    /**
     * Called when an RPGMobs elite dies.
     */
    default void onRPGMobDeath(RPGMobsDeathEvent event) {
    }

    /**
     * Called after an elite dies, before loot is spawned into the world.
     * Cancellable - the drop list can be modified or the event cancelled entirely.
     */
    default void onRPGMobDrops(RPGMobsDropsEvent event) {
    }

    /**
     * Called when an RPGMobs elite deals damage to another entity.
     * Cancellable - the damage multiplier can be adjusted or the event cancelled.
     */
    default void onRPGMobDamageDealt(RPGMobsDamageDealtEvent event) {
    }

    /**
     * Called when an RPGMobs elite receives damage from any source.
     * Informational only - cannot be cancelled or modified.
     */
    default void onRPGMobDamageReceived(RPGMobsDamageReceivedEvent event) {
    }

    /**
     * Called when RPGMobs configuration is reloaded and entities are reconciled.
     */
    default void onReconcile(RPGMobsReconcileEvent event) {
    }

    /**
     * Called when an elite begins an ability chain (e.g. charge_leap, multi_slash_short).
     * Cancellable - prevents the ability from starting.
     */
    default void onRPGMobAbilityStarted(RPGMobsAbilityStartedEvent event) {
    }

    /**
     * Called when an ability chain finishes successfully.
     */
    default void onRPGMobAbilityCompleted(RPGMobsAbilityCompletedEvent event) {
    }

    /**
     * Called when an ability chain is interrupted (e.g. by death or deaggro).
     */
    default void onRPGMobAbilityInterrupted(RPGMobsAbilityInterruptedEvent event) {
    }

    /**
     * Called after health, damage, and model scaling are applied to an elite.
     */
    default void onScalingApplied(RPGMobsScalingAppliedEvent event) {
    }

    /**
     * Called when an RPGMobs elite acquires a combat target (enters combat).
     */
    default void onRPGMobAggro(RPGMobsAggroEvent event) {
    }

    /**
     * Called when an RPGMobs elite loses its combat target (leaves combat).
     */
    default void onRPGMobDeaggro(RPGMobsDeaggroEvent event) {
    }
}
