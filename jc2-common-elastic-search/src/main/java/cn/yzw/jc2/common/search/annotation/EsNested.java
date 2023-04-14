package cn.yzw.jc2.common.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES nested
 *
 * @author: zhangzhibao
 * @version: 1.0.0
 * @date: 2022-08-11 20:00
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsNested {
    /**
     * filed name
     */
    String name() default "";

    /**
     * inner hits 启用
     * @return
     */
    boolean needInnerHits() default false;

    /**
     * inner hits 返回size
     * @return
     */
    int innerHitsSize() default 100;

}
