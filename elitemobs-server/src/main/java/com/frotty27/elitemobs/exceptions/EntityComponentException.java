package com.frotty27.elitemobs.exceptions;

import java.io.Serial;

public class EntityComponentException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EntityComponentException(String componentName, int entityId) {
        super("Missing component " + componentName + " on entity " + entityId);
    }
}
