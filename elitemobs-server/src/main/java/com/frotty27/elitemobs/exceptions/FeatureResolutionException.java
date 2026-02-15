package com.frotty27.elitemobs.exceptions;

import java.io.Serial;

public class FeatureResolutionException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public FeatureResolutionException(String id) {
        super("Feature not found: " + id);
    }
}
