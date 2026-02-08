package com.frotty27.elitemobs.exception;

import java.io.Serial;

/**
 * Thrown when a feature cannot be resolved by ID or Key.
 */
public class FeatureResolutionException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FeatureResolutionException(String id) {
        super("Feature not found: " + id);
    }
}
