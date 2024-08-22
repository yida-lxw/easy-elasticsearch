package cn.yzw.jc2.common.search.annotation.agg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description: 去重计数
 * @Author: lbl 
 * @Date: 2023/6/25
 **/
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsCardinality {
    /**
     * 聚合名称
     */
    String aggName() default "";

    /**
     * filed name
     */
    String name() default "";

}
