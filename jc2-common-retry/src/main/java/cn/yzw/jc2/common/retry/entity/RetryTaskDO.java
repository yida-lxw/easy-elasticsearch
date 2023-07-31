package cn.yzw.jc2.common.retry.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RetryTaskDO implements Serializable {

    /**
     * 表名
     */
    private String tableName;
    /**
     * 主键ID
     */
    private Long    id;

    /**
     * 创建时间
     */
    private Date    createTime;

    /**
     * 更新时间
     */
    private Date    updateTime;

    /**
     * 重试任务唯一编号
     */
    private String  retryTaskNo;

    /**
     * 业务key,关联代码里的重试方法
     */
    private String  bizKey;

    /**
     * 任务计划执行时间
     */
    private Date    taskPlanExecTime;

    /**
     * 任务执行开始时间
     */
    private Date    taskStartTime;

    /**
     * 任务执行状态：PENDING-待执行,EXECUTING-执行中,FAIL-执行失败
     */
    private String  taskExecStatus;

    /**
     * 任务执行次数
     */
    private Integer taskExecCount;

    /**
     * 任务数据
     */
    private String  taskData;

    /**
     * 执行信息
     */
    private String  taskExecMsg;

    /**
     * 任务唯一序列号，
     * 序列号相同的任务根据ID顺序串行执行，例如同一个业务单据，发送代办，完成代办，撤销代办
     */
    private String  bizSequenceNo;

    /**
     * 业务执行优先级,如果设置了序列号，那么优先级必须设置，不设置不保证顺序执行，默认为0
     */
    private Integer bizSequencePriority;

    /**
     * 上一个优先级的业务是非执行成功的状态或者不存在，那么当前业务创建后，允许多久时间后可执行
     * 时间单位为秒
     * 如果不设置，上一个业务没执行成功，当前业务不允许执行
     */
    private Long    bizSequenceCanExecSecondTime;
    /**
     * 低优先记业务是否允许执行
     * 优先级高的任务不存在，优先级低的任务是否允许执行，例如bizSequencePriority=2，但是不存在<2的任务，2是否可以执行
     * NO表示不可以执行，YES表示可以执行，默认为YES,可以执行
     * RetryTaskPriorityCheckEnums
     */
    private String  bizSequenceLowPriorityCanExec;

    /**
     * 环境标识，主要用来区分线上和stg环境，默认为空
     */
    private String  retryEnvFlag;
}