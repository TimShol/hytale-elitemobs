package com.frotty27.rpgmobs.api;

import com.frotty27.rpgmobs.api.query.IRPGMobsQueryAPI;

/**
 * Primary entry point for the RPGMobs API.
 *
 * <p>Provides static accessors for registering event listeners and querying RPG mob state.
 * Before calling any method on this class, ensure that the RPGMobs mod is installed and
 * your {@code manifest.json} declares {@code "Frotty27:RPGMobs": "*"} under
 * {@code Dependencies}.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * // Register a listener
 * RPGMobsAPI.registerListener(myListener);
 *
 * // Query mob data
 * Optional<Integer> tier = RPGMobsAPI.query().getTier(entityRef);
 * }</pre>
 *
 * @since 1.1.0
 */
public final class RPGMobsAPI {

    private static volatile IRPGMobsEventBus eventBus;
    private static volatile IRPGMobsQueryAPI queryAPI;

    private RPGMobsAPI() {
    }

    /**
     * Sets the event bus implementation. This is called internally by the RPGMobs mod
     * during initialization and should not be called by external consumers.
     *
     * @param bus the event bus implementation to use
     */
    public static void setEventBus(IRPGMobsEventBus bus) {
        RPGMobsAPI.eventBus = bus;
    }

    /**
     * Sets the query API implementation. This is called internally by the RPGMobs mod
     * during initialization and should not be called by external consumers.
     *
     * @param api the query API implementation to use
     */
    public static void setQueryAPI(IRPGMobsQueryAPI api) {
        RPGMobsAPI.queryAPI = api;
    }

    /**
     * Registers an event listener to receive RPGMobs events.
     *
     * <p>The listener will be notified of all events it has overridden handlers for,
     * such as spawns, deaths, damage, drops, abilities, and aggro changes.</p>
     *
     * @param listener the listener to register; must not be {@code null}
     * @throws IllegalArgumentException       if {@code listener} is {@code null}
     * @throws RPGMobsNotInitializedException if the API has not been initialized yet
     */
    public static void registerListener(IRPGMobsEventListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        getEventBus().registerListener(listener);
    }

    /**
     * Unregisters a previously registered event listener.
     *
     * <p>After this call, the listener will no longer receive any RPGMobs events.</p>
     *
     * @param listener the listener to unregister; must not be {@code null}
     * @throws IllegalArgumentException       if {@code listener} is {@code null}
     * @throws RPGMobsNotInitializedException if the API has not been initialized yet
     */
    public static void unregisterListener(IRPGMobsEventListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        getEventBus().unregisterListener(listener);
    }

    /**
     * Returns the query API for inspecting RPG mob state.
     *
     * <p>Use this to look up tier, health multipliers, damage multipliers,
     * combat status, and other properties of RPG Mobs.</p>
     *
     * @return the query API instance, never {@code null}
     * @throws RPGMobsNotInitializedException if the API has not been initialized yet
     */
    public static IRPGMobsQueryAPI query() {
        IRPGMobsQueryAPI current = queryAPI;
        if (current == null) {
            throw new RPGMobsNotInitializedException("RPGMobs API not initialized. " + "Ensure RPGMobs is installed and your manifest.json declares " + "\"Frotty27:RPGMobs\": \"*\" in Dependencies.");
        }
        return current;
    }

    /**
     * Returns the event bus used for listener registration.
     *
     * <p>In most cases, prefer using {@link #registerListener(IRPGMobsEventListener)}
     * and {@link #unregisterListener(IRPGMobsEventListener)} instead of accessing
     * the event bus directly.</p>
     *
     * @return the event bus instance, never {@code null}
     * @throws RPGMobsNotInitializedException if the API has not been initialized yet
     */
    public static IRPGMobsEventBus getEventBus() {
        IRPGMobsEventBus current = eventBus;
        if (current == null) {
            throw new RPGMobsNotInitializedException("RPGMobs API not initialized. " + "Ensure RPGMobs is installed and your manifest.json declares " + "\"Frotty27:RPGMobs\": \"*\" in Dependencies.");
        }
        return current;
    }
}
