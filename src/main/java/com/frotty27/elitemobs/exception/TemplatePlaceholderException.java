package com.frotty27.elitemobs.exception;

import java.io.Serial;

/**
 * Thrown when a placeholder ${...} cannot be resolved.
 */
public class TemplatePlaceholderException extends TemplateEvaluationException {
    @Serial
    private static final long serialVersionUID = 1L;

    public TemplatePlaceholderException(String placeholder, String context) {
        super("Failed to resolve placeholder: " + placeholder + " (" + context + ")");
    }
}
