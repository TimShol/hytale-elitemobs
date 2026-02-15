package com.frotty27.elitemobs.exceptions;

import java.io.Serial;

public class EliteMobsSystemException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EliteMobsSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
