package com.frotty27.rpgmobs.exceptions;

import java.io.Serial;

public class RPGMobsSystemException extends RPGMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public RPGMobsSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
