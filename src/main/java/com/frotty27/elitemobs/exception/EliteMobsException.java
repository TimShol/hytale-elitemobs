package com.frotty27.elitemobs.exception;

import java.io.Serial;

/**
 * Base class for all EliteMobs specific exceptions.
 * Provides capabilities for attaching context to errors.
 */
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
