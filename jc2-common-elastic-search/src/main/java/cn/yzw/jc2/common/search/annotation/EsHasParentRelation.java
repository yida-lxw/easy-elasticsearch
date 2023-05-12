package cn.yzw.jc2.common.search.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ES 关系（父子）查询,has_parent
 *
 * @author: lbl
 * @version: 1.0.0
 * @date: 2022-11-28 20:00
 */
@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EsHasParentRelation {

    /**
     * 
     * 父节点类型key
     * @return
     */
    String parentType() default "";

    /**
     * inner hit size
     * @return
     */
    int innerHitsSize() default 100;

    /**
     * inner name
     * @return
     */
    String innerHitsName();
}
