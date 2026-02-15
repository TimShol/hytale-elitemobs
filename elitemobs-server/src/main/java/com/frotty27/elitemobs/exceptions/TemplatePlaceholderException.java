package com.frotty27.elitemobs.exceptions;

import java.io.Serial;

public class TemplatePlaceholderException extends TemplateEvaluationException {
    @Serial
    private static final long serialVersionUID = 1L;

    public TemplatePlaceholderException(String placeholder, String context) {
        super("Failed to resolve placeholder: " + placeholder + " (" + context + ")");
    }
}
