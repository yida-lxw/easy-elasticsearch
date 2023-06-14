package cn.yzw.jc2.common.search.annotation.agg;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsMax {
    /**
     * 聚合名称
     */
    String aggName() default "";
    /**
     * filed name
     */
    String name() default "";

}
