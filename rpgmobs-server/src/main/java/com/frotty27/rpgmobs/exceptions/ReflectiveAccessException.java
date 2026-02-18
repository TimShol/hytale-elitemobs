package com.frotty27.rpgmobs.exceptions;

import java.io.Serial;

public class ReflectiveAccessException extends TemplateEvaluationException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ReflectiveAccessException(String field, Throwable cause) {
        super("Failed to access field: " + field, cause);
    }
}
