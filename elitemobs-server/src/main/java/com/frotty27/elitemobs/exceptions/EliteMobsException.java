package com.frotty27.elitemobs.exceptions;

import java.io.Serial;

public abstract class EliteMobsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected EliteMobsException(String message) {
        super(message);
    }

    protected EliteMobsException(String message, Throwable cause) {
        super(message, cause);
    }
}
