package cn.yzw.jc2.common.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES match
 *
 * @author: zhangzhibao
 * @version: 1.0.0
 * @date: 2022-08-11 20:02
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsMatch {
    /**
     * filed name
     */
    String name() default "";
}