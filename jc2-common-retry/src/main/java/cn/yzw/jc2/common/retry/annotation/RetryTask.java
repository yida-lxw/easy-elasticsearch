package cn.yzw.jc2.common.retry.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description: 重试任务注解，打了次注解的方法，会被重试任务执行
 * @Author: lbl
 * @Date: 2023/4/27
 **/
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

    /**
     * 重试任务异步回调的时候，是否需要回传租户，
     * 默认不需要，如果开启，会将tenantId返回，与此同时，
     * 业务方法需要定义为2个参数，示例如下
     * @RetryTask(value = RetryBizKeyConstant.PLAN_COMPLETE_TODO_BY_BIZ_BATCH_NO,needReturnTenantId = true)
     * public void completeTodo(String param, String tenantId) {
     * }
     */
    boolean needReturnTenantId() default false;

}
