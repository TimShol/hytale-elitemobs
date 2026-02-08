package com.frotty27.elitemobs.exception;

import java.io.Serial;

/**
 * Thrown when a feature fails to register (e.g. duplicate keys).
 */
public class FeatureRegistrationException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FeatureRegistrationException(String message) {
        super(message);
    }
}
