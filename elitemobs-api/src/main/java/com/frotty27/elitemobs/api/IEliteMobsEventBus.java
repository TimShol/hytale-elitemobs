package com.frotty27.elitemobs.api;

/**
 * Event bus for the EliteMobs mod, responsible for managing event listener registration.
 *
 * <p>Implementations dispatch events to all registered {@link IEliteMobsEventListener}
 * instances. In most cases, consumers should use the convenience methods on
 * {@link EliteMobsAPI} rather than interacting with this interface directly.</p>
 *
 * @see EliteMobsAPI#registerListener(IEliteMobsEventListener)
 * @see EliteMobsAPI#unregisterListener(IEliteMobsEventListener)
 * @since 1.1.0
 */
public interface IEliteMobsEventBus {

    /**
     * Registers an event listener to receive EliteMobs events.
     *
     * @param listener the listener to register
     */
    void registerListener(IEliteMobsEventListener listener);

    /**
     * Unregisters a previously registered event listener so it no longer receives events.
     *
     * @param listener the listener to unregister
     */
    void unregisterListener(IEliteMobsEventListener listener);
}
