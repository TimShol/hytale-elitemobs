package com.frotty27.rpgmobs.api.events;

/**
 * Marker interface for events that can be cancelled by listeners.
 *
 * <p>When an event is cancelled, the action that triggered it is prevented.
 * For example, cancelling a {@link RPGMobsSpawnedEvent} prevents the NPC
 * from becoming an elite.</p>
 *
 * @since 1.0.0
 */
public interface ICancellable {

    /**
     * Returns whether this event has been cancelled.
     *
     * @return true if cancelled
     */
    boolean isCancelled();

    /**
     * Sets the cancellation state of this event.
     *
     * @param cancelled true to cancel, false to un-cancel
     */
    void setCancelled(boolean cancelled);
}
