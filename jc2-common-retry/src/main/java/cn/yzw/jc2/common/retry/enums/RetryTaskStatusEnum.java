package cn.yzw.jc2.common.retry.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RetryTaskStatusEnum {
    /**
     * 待执行
     */
    PENDING("待执行"),
    /**
     * 执行中
     */
    EXECUTING("执行中"),
    /**
     * 执行失败
     */
    FAIL("执行失败"),
    /**
     * 执行成功
     */
    SUCCESS("执行成功");
    private final String message;
}
