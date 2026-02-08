package com.frotty27.elitemobs.exception;

import java.io.Serial;

/**
 * Generic wrapper for exceptions thrown within EliteMobs systems.
 */
public class EliteMobsSystemException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EliteMobsSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
