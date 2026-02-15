package com.frotty27.elitemobs.api;

/**
 * Thrown when the EliteMobs API is accessed before the mod has completed initialization.
 *
 * <p>This typically indicates that:</p>
 * <ul>
 *   <li>The EliteMobs mod is not installed on the server.</li>
 *   <li>Your {@code manifest.json} does not declare {@code "Frotty27:EliteMobs": "*"}
 *       under {@code Dependencies}, so load ordering is not guaranteed.</li>
 *   <li>The API is being accessed too early during server startup.</li>
 * </ul>
 *
 * @since 1.1.0
 */
public final class EliteMobsNotInitializedException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message explaining why initialization failed
     */
    public EliteMobsNotInitializedException(String message) {
        super(message);
    }
}
