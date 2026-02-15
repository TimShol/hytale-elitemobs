package com.frotty27.elitemobs.exceptions;

import java.io.Serial;

public abstract class TemplateEvaluationException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    protected TemplateEvaluationException(String message) {
        super(message);
    }

    protected TemplateEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
