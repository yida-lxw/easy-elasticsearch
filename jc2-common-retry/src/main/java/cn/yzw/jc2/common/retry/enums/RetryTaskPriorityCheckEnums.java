package cn.yzw.jc2.common.retry.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RetryTaskPriorityCheckEnums {
    /**
     *
     */
    YES("允许执行"),
    /**
     * 执行中
     */
    NO("不允许执行");
    /**
     * 执行失败
     */
    private final String message;
}
