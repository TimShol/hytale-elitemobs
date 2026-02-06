package com.frotty27.elitemobs.config.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the configuration version.
 * - Its value is taken from the Java instance (the "target" version).
 * - It is written to the YAML file.
 * - It is NEVER read from the YAML file (so the file always updates to the code's version).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CfgVersion {
}
