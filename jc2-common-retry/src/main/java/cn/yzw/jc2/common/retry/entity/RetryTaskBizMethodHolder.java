package cn.yzw.jc2.common.retry.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RetryTaskBizMethodHolder {
    /**
     * 目标服务
     */
    private Object targetService;
    /**
     * 业务执行方法
     */
    private Method bizMethod;
    /**
     * 分布式锁超时时间
     */
    private Long lockSeconds;

    /**
     * 重试任务异步回调的时候，是否需要回传租户，
     * 默认不需要，如果开启，会将tenantId返回，与此同时，
     * 业务方法需要定义为2个参数，示例如下
     */
    private boolean needReturnTenantId;
}
