package cn.yzw.jc2.common.retry.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RetryCreateTask implements Serializable {

    /**
     * 重试任务唯一编号，使用
     */
    private String  retryTaskNo;

    /**
     * 业务key,关联代码里的重试方法，不同的业务不能重复
     */
    private String  bizKey;

    /**
     * 任务计划执行时间
     */
    private Date    taskPlanExecTime;

    /**
     * 任务数据
     */
    private String  taskData;

    /**
     * 业务唯一key或者编号，唯一key相同的任务根据ID顺序串行执行
     */
    private String  bizSequenceKey;

    /**
     * 业务执行优先级,如果设置了业务串行key，那么优先级必须设置，不设置不保证顺序执行，默认为0
     */
    private Integer bizSequencePriority;
    /**
     * 上一业务执行优先级，当前优先级的上一个优先级,不设置默认当前优先级-1
     */
    private Integer bizSequencePriorityPrev;

    /**
     * 允许执行时间范围
     * 上一个优先级的业务是非执行成功的状态或者不存在，那么当前业务创建后，允许多久时间后可执行
     * 时间单位为秒
     * 如果不设置，上一个业务不存在或者没执行成功，当前业务不允许执行
     */
    private Long    bizSequenceCanExeSecondTime;

    /**
     * 是否校验上一级，默认不校验，
     */
    private Boolean bizSequencePrevCheck;
}