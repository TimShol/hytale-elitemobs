package com.frotty27.elitemobs.config.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Cfg {
    String file();                 
    String key() default "";       
    String group() default "";     
    String comment() default "";   
}
