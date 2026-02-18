package com.frotty27.rpgmobs.exceptions;

import java.io.Serial;

public class FeatureConfigurationException extends RPGMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FeatureConfigurationException(String message) {
        super(message);
    }
}
