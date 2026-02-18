package com.frotty27.rpgmobs.api;

/**
 * Thrown when the RPGMobs API is accessed before the mod has completed initialization.
 *
 * <p>This typically indicates that:</p>
 * <ul>
 *   <li>The RPGMobs mod is not installed on the server.</li>
 *   <li>Your {@code manifest.json} does not declare {@code "Frotty27:RPGMobs": "*"}
 *       under {@code Dependencies}, so load ordering is not guaranteed.</li>
 *   <li>The API is being accessed too early during server startup.</li>
 * </ul>
 *
 * @since 1.1.0
 */
public final class RPGMobsNotInitializedException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message explaining why initialization failed
     */
    public RPGMobsNotInitializedException(String message) {
        super(message);
    }
}
