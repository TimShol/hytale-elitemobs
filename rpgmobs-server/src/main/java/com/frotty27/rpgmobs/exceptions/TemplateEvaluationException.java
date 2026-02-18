package com.frotty27.rpgmobs.exceptions;

import java.io.Serial;

public abstract class TemplateEvaluationException extends RPGMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected TemplateEvaluationException(String message) {
        super(message);
    }

    protected TemplateEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
