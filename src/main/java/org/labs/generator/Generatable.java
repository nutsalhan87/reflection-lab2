package org.labs.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.CONSTRUCTOR})
public @interface Generatable {
    public final static long DEFAULT_MIN = -10000;
    public final static long DEFAULT_MAX = 10000;
    
    long min() default DEFAULT_MIN;
    long max() default DEFAULT_MAX;
}
