package com.frotty27.elitemobs.exception;

import java.io.Serial;

/**
 * Thrown when a required component is missing from an entity.
 */
public class EntityComponentException extends EliteMobsException {
    @Serial
    private static final long serialVersionUID = 1L;

    public EntityComponentException(String componentName, int entityId) {
        super("Missing component " + componentName + " on entity " + entityId);
    }
}
