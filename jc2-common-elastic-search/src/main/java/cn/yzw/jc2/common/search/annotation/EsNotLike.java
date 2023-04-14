package cn.yzw.jc2.common.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES not like
 *
 * @author: liurun
 * @date: 2023-01-11 18:02
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsNotLike {
    /**
     * filed name
     */
    String name() default "";

    boolean leftLike() default false;

    boolean rightLike() default false;
}