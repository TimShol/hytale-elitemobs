package com.frotty27.elitemobs.exception;

import java.io.Serial;

/**
 * Thrown when reflective access to config fields fails.
 */
public class ReflectiveAccessException extends TemplateEvaluationException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ReflectiveAccessException(String field, Throwable cause) {
        super("Failed to access field: " + field, cause);
    }
}
