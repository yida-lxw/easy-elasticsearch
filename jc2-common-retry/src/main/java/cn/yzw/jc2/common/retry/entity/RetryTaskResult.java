package cn.yzw.jc2.common.retry.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @Description: 重试任务执行结束，返回结果
 * @Author: lbl 
 * @Date: 2024/8/14
 **/
@Data
public class RetryTaskResult<T> implements Serializable {
    /**
     * true:执行成功
     */
    private boolean success;
    /**
     * 提示信息，执行成功与否，都会存
     */
    private String  msg;

    /**
     * 重试任务本身暂时没用到
     */
    private T       data;
}
