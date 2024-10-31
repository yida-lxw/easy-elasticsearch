package com.easy.elastic.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES like
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2022-08-11 20:02
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsLike {
    /**
     * filed name
     */
    String name() default "";

    boolean leftLike() default false;

    boolean rightLike() default false;
}