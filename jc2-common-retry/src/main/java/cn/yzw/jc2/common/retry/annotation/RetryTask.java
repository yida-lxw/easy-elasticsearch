package cn.yzw.jc2.common.retry.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RetryTask {
    /**
     * 业务key，业务执行方法通过这个key注册到注册表中
     * @return
     */
    String value();

    /**
     * 业务方法分布式锁超时时间
     * @return
     */
    long lockSeconds() default 600L;
}
