package com.frotty27.rpgmobs.api.events;

/**
 * Event fired during a reconciliation pass.
 *
 * <p>Reconciliation allows listeners to synchronize their internal state with the
 * current RPG mob data. This is typically triggered after configuration reloads
 * or when the system needs to ensure consistency across all registered listeners.</p>
 *
 * @since 1.1.0
 */
public final class RPGMobsReconcileEvent {

    /**
     * Constructs a new reconcile event.
     */
    public RPGMobsReconcileEvent() {
    }
}
