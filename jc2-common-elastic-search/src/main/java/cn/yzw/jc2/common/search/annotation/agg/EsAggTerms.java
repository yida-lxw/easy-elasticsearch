package cn.yzw.jc2.common.search.annotation.agg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author liangbaole
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsAggTerms {
    /**
     * 聚合名称
     */
    String aggName() default "";

    /**
     * filed name
     */
    String name() default "";

    /**
     * 返回size
     * @return
     */
    int size() default 10;

    /**
     * 是否有嵌套聚合
     * @return
     */
    boolean hasSubAgg() default false;
}
