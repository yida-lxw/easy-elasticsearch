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
}
