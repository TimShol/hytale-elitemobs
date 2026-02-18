package com.frotty27.rpgmobs.exceptions;

import java.io.Serial;

public class TemplateSyntaxException extends TemplateEvaluationException {
    @Serial
    private static final long serialVersionUID = 1L;

    public TemplateSyntaxException(String message) {
        super(message);
    }
}
