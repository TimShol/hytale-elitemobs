package com.frotty27.rpgmobs.exceptions;

import java.io.Serial;

public abstract class RPGMobsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected RPGMobsException(String message) {
        super(message);
    }

    protected RPGMobsException(String message, Throwable cause) {
        super(message, cause);
    }
}
