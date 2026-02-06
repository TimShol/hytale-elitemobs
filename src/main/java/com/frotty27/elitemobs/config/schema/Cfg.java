package com.frotty27.elitemobs.config.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Cfg {
    String file();                 // "main.yml", "catalogs.yml", ...
    String key() default "";       // optional override; default = field name
    String group() default "";     // Which group to put the property under
    String comment() default "";   // YAML comments
}
