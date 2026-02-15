package com.frotty27.elitemobs.api;

import com.frotty27.elitemobs.api.query.IEliteMobsQueryAPI;

/**
 * Primary entry point for the EliteMobs API.
 *
 * <p>Provides static accessors for registering event listeners and querying elite mob state.
 * Before calling any method on this class, ensure that the EliteMobs mod is installed and
 * your {@code manifest.json} declares {@code "Frotty27:EliteMobs": "*"} under
 * {@code Dependencies}.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * // Register a listener
 * EliteMobsAPI.registerListener(myListener);
 *
 * // Query mob data
 * Optional<Integer> tier = EliteMobsAPI.query().getTier(entityRef);
 * }</pre>
 *
 * @since 1.1.0
 */
public final class EliteMobsAPI {

    private static volatile IEliteMobsEventBus eventBus;
    private static volatile IEliteMobsQueryAPI queryAPI;

    private EliteMobsAPI() {}

    /**
     * Sets the event bus implementation. This is called internally by the EliteMobs mod
     * during initialization and should not be called by external consumers.
     *
     * @param bus the event bus implementation to use
     */
    public static void setEventBus(IEliteMobsEventBus bus) {
        EliteMobsAPI.eventBus = bus;
    }

    /**
     * Sets the query API implementation. This is called internally by the EliteMobs mod
     * during initialization and should not be called by external consumers.
     *
     * @param api the query API implementation to use
     */
    public static void setQueryAPI(IEliteMobsQueryAPI api) {
        EliteMobsAPI.queryAPI = api;
    }

    /**
     * Registers an event listener to receive EliteMobs events.
     *
     * <p>The listener will be notified of all events it has overridden handlers for,
     * such as spawns, deaths, damage, drops, abilities, and aggro changes.</p>
     *
     * @param listener the listener to register; must not be {@code null}
     * @throws IllegalArgumentException if {@code listener} is {@code null}
     * @throws EliteMobsNotInitializedException if the API has not been initialized yet
     */
    public static void registerListener(IEliteMobsEventListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        getEventBus().registerListener(listener);
    }

    /**
     * Unregisters a previously registered event listener.
     *
     * <p>After this call, the listener will no longer receive any EliteMobs events.</p>
     *
     * @param listener the listener to unregister; must not be {@code null}
     * @throws IllegalArgumentException if {@code listener} is {@code null}
     * @throws EliteMobsNotInitializedException if the API has not been initialized yet
     */
    public static void unregisterListener(IEliteMobsEventListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        getEventBus().unregisterListener(listener);
    }

    /**
     * Returns the query API for inspecting elite mob state.
     *
     * <p>Use this to look up tier, health multipliers, damage multipliers,
     * combat status, and other properties of elite mobs.</p>
     *
     * @return the query API instance, never {@code null}
     * @throws EliteMobsNotInitializedException if the API has not been initialized yet
     */
    public static IEliteMobsQueryAPI query() {
        IEliteMobsQueryAPI current = queryAPI;
        if (current == null) {
            throw new EliteMobsNotInitializedException(
                    "EliteMobs API not initialized. "
                            + "Ensure EliteMobs is installed and your manifest.json declares "
                            + "\"Frotty27:EliteMobs\": \"*\" in Dependencies.");
        }
        return current;
    }

    /**
     * Returns the event bus used for listener registration.
     *
     * <p>In most cases, prefer using {@link #registerListener(IEliteMobsEventListener)}
     * and {@link #unregisterListener(IEliteMobsEventListener)} instead of accessing
     * the event bus directly.</p>
     *
     * @return the event bus instance, never {@code null}
     * @throws EliteMobsNotInitializedException if the API has not been initialized yet
     */
    public static IEliteMobsEventBus getEventBus() {
        IEliteMobsEventBus current = eventBus;
        if (current == null) {
            throw new EliteMobsNotInitializedException(
                    "EliteMobs API not initialized. "
                            + "Ensure EliteMobs is installed and your manifest.json declares "
                            + "\"Frotty27:EliteMobs\": \"*\" in Dependencies.");
        }
        return current;
    }
}
