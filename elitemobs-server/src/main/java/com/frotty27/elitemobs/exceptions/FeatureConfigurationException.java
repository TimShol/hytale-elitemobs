package com.frotty27.elitemobs.exceptions;

import java.io.Serial;

public class FeatureConfigurationException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FeatureConfigurationException(String message) {
        super(message);
    }
}
