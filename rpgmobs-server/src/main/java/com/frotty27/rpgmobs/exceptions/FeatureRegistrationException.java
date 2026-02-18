package com.frotty27.rpgmobs.exceptions;

import java.io.Serial;

public class FeatureRegistrationException extends RPGMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FeatureRegistrationException(String message) {
        super(message);
    }
}
