package com.easy.elastic.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES multi 多条件组合查询
 *
 * @author: liangbaole
 * @version: 1.0.0
 * @date: 2024-08-11 20:00
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsMulti {
    /**
     * 嵌套之间是否为与的关系，默认为或
     * @return
     */
    boolean isAnd() default false;
}
