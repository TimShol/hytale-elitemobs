package com.frotty27.rpgmobs.api;

import com.frotty27.rpgmobs.api.query.IRPGMobsQueryAPI;

/**
 * Main entry point for the RPGMobs public API.
 *
 * <p>Provides static access to the event bus (for listening to RPGMobs events)
 * and the query API (for reading mob state). Both are initialized by the RPGMobs
 * server plugin during startup.</p>
 *
 * <h3>Quick Start</h3>
 * <pre>{@code
 * // Listen for elite mob spawns
 * RPGMobsAPI.registerListener(new IRPGMobsEventListener() {
 *     @Override
 *     public void onRPGMobSpawned(RPGMobsSpawnedEvent event) {
 *         System.out.println("Elite spawned: " + event.getRoleName() + " tier " + (event.getTier() + 1));
 *     }
 * });
 *
 * // Query mob state
 * Optional<Integer> tier = RPGMobsAPI.query().getTier(entityRef);
 * }</pre>
 *
 * <h3>Dependency</h3>
 * <p>Your {@code manifest.json} must declare {@code "Frotty27:RPGMobs": "*"} in
 * {@code Dependencies} to ensure RPGMobs loads before your plugin.</p>
 *
 * @since 1.0.0
 */
public final class RPGMobsAPI {

    private static volatile IRPGMobsEventBus eventBus;
    private static volatile IRPGMobsQueryAPI queryAPI;

    private RPGMobsAPI() {
    }

    /**
     * Sets the event bus implementation. Called internally by RPGMobs during startup.
     *
     * @param bus the event bus implementation
     */
    public static void setEventBus(IRPGMobsEventBus bus) {
        RPGMobsAPI.eventBus = bus;
    }

    /**
     * Sets the query API implementation. Called internally by RPGMobs during startup.
     *
     * @param api the query API implementation
     */
    public static void setQueryAPI(IRPGMobsQueryAPI api) {
        RPGMobsAPI.queryAPI = api;
    }

    /**
     * Registers an event listener to receive RPGMobs events.
     *
     * @param listener the listener to register (must not be null)
     * @throws IllegalArgumentException if listener is null
     * @throws RPGMobsNotInitializedException if RPGMobs has not finished loading
     * @see IRPGMobsEventListener
     */
    public static void registerListener(IRPGMobsEventListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        getEventBus().registerListener(listener);
    }

    /**
     * Unregisters a previously registered event listener.
     *
     * @param listener the listener to unregister (must not be null)
     * @throws IllegalArgumentException if listener is null
     * @throws RPGMobsNotInitializedException if RPGMobs has not finished loading
     */
    public static void unregisterListener(IRPGMobsEventListener listener) {
        if (listener == null) throw new IllegalArgumentException("listener must not be null");
        getEventBus().unregisterListener(listener);
    }

    /**
     * Returns the query API for reading RPGMobs entity state.
     *
     * @return the query API instance
     * @throws RPGMobsNotInitializedException if RPGMobs has not finished loading
     * @see IRPGMobsQueryAPI
     */
    public static IRPGMobsQueryAPI query() {
        IRPGMobsQueryAPI current = queryAPI;
        if (current == null) {
            throw new RPGMobsNotInitializedException("RPGMobs API not initialized. " + "Ensure RPGMobs is installed and your manifest.json declares " + "\"Frotty27:RPGMobs\": \"*\" in Dependencies.");
        }
        return current;
    }

    /**
     * Returns the event bus for advanced listener management.
     *
     * <p>For simple listener registration, prefer {@link #registerListener(IRPGMobsEventListener)}.</p>
     *
     * @return the event bus instance
     * @throws RPGMobsNotInitializedException if RPGMobs has not finished loading
     */
    public static IRPGMobsEventBus getEventBus() {
        IRPGMobsEventBus current = eventBus;
        if (current == null) {
            throw new RPGMobsNotInitializedException("RPGMobs API not initialized. " + "Ensure RPGMobs is installed and your manifest.json declares " + "\"Frotty27:RPGMobs\": \"*\" in Dependencies.");
        }
        return current;
    }
}
