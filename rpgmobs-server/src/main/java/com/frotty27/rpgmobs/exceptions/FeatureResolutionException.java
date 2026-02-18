package com.frotty27.rpgmobs.exceptions;

import java.io.Serial;

public class FeatureResolutionException extends RPGMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FeatureResolutionException(String id) {
        super("Feature not found: " + id);
    }
}
