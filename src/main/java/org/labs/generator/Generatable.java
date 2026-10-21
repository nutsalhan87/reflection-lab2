package org.labs.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE})
public @interface Generatable {
    long min() default Constants.MIN;
    long max() default Constants.MAX;
    int minLength() default Constants.MIN_LENGTH;
    int maxLength() default Constants.DEFAULT_LENGTH_BOUND;
}
