package com.easy.elastic.search.annotation.agg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description: 聚合nested
 * @Author: liangbaole
 * @Date: 2023/6/25
 **/
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsAggNested {

    /**
     * 聚合名称
     */
    String aggName();

    /**
     * filed name
     */
    String name() default "";
}
