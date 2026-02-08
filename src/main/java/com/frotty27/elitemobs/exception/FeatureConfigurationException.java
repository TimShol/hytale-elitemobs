package com.frotty27.elitemobs.exception;

import java.io.Serial;

/**
 * Thrown if a Feature's configuration cannot be retrieved or is invalid.
 */
public class FeatureConfigurationException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FeatureConfigurationException(String message) {
        super(message);
    }
}
