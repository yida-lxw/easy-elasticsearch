package cn.yzw.jc2.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES 关系（父子）查询
 *
 * @author: zhangzhibao
 * @version: 1.0.0
 * @date: 2022-11-28 20:00
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsRelationQuery {

    /**
     * 
     * 子节点类型key
     * @return
     */
    String childType() default "";

    /**
     * inner hit size
     * @return
     */
    int innerHitsSize() default 100;
}
