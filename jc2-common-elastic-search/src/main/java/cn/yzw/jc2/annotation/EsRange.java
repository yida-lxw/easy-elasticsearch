package cn.yzw.jc2.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES like
 *
 * @author: zhangzhibao
 * @version: 1.0.0
 * @date: 2022-08-11 20:02
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsRange {
    /**
     * filed name
     */
    String name() default "";

    /**
     * >
     */
    boolean lt() default false;

    /**
     * <
     */
    boolean gt() default false;

    /**
     * 包含上界
     */
    boolean includeUpper() default false;

    /**
     * 包含下界
     */
    boolean includeLower() default false;
}