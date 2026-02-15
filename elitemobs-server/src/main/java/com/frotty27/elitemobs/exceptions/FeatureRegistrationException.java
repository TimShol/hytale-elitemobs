package com.frotty27.elitemobs.exceptions;

import java.io.Serial;

public class FeatureRegistrationException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FeatureRegistrationException(String message) {
        super(message);
    }
}
