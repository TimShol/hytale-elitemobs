package com.frotty27.elitemobs.exception;

import java.io.Serial;

/**
 * Thrown when template syntax is invalid.
 */
public class TemplateSyntaxException extends TemplateEvaluationException {
    @Serial
    private static final long serialVersionUID = 1L;

    public TemplateSyntaxException(String message) {
        super(message);
    }
}
