package com.frotty27.rpgmobs.api;

/**
 * Thrown when the RPGMobs API is accessed before the plugin has initialized.
 *
 * <p>This typically means your plugin loaded before RPGMobs. Ensure your
 * {@code manifest.json} declares {@code "Frotty27:RPGMobs": "*"} in
 * {@code Dependencies}.</p>
 *
 * @since 1.0.0
 */
public final class RPGMobsNotInitializedException extends RuntimeException {

    /**
     * @param message detail message describing what was accessed before initialization
     */
    public RPGMobsNotInitializedException(String message) {
        super(message);
    }
}
