package com.frotty27.rpgmobs.api;

/**
 * Event bus for RPGMobs event dispatch and listener management.
 *
 * <p>For most use cases, prefer the convenience methods on {@link RPGMobsAPI}
 * rather than interacting with the event bus directly.</p>
 *
 * @see RPGMobsAPI#registerListener(IRPGMobsEventListener)
 * @since 1.0.0
 */
public interface IRPGMobsEventBus {

    /**
     * Registers an event listener.
     *
     * @param listener the listener to register
     */
    void registerListener(IRPGMobsEventListener listener);

    /**
     * Unregisters a previously registered event listener.
     *
     * @param listener the listener to unregister
     */
    void unregisterListener(IRPGMobsEventListener listener);
}
