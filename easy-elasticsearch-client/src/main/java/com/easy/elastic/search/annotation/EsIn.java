package com.easy.elastic.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES IN
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2024-08-11 20:01
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsIn {
    /**
     * filed name
     */
    String name() default "";

    boolean leftLike() default false;

    boolean rightLike() default false;

    /**
     * 为空的时候，转空列表
     * @return
     */
    boolean allowEmpty() default false;
}