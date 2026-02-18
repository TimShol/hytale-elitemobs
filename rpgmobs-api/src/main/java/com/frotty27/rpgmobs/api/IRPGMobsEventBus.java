package com.frotty27.rpgmobs.api;

/**
 * Event bus for the RPGMobs mod, responsible for managing event listener registration.
 *
 * <p>Implementations dispatch events to all registered {@link IRPGMobsEventListener}
 * instances. In most cases, consumers should use the convenience methods on
 * {@link RPGMobsAPI} rather than interacting with this interface directly.</p>
 *
 * @see RPGMobsAPI#registerListener(IRPGMobsEventListener)
 * @see RPGMobsAPI#unregisterListener(IRPGMobsEventListener)
 * @since 1.1.0
 */
public interface IRPGMobsEventBus {

    /**
     * Registers an event listener to receive RPGMobs events.
     *
     * @param listener the listener to register
     */
    void registerListener(IRPGMobsEventListener listener);

    /**
     * Unregisters a previously registered event listener so it no longer receives events.
     *
     * @param listener the listener to unregister
     */
    void unregisterListener(IRPGMobsEventListener listener);
}
