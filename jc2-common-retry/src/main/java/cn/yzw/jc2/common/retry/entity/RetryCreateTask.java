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
     * 重试任务唯一编号，需要接入方使用分布式id生成器生成
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
     * 业务执行优先级,如果设置了业务串行key，那么优先级必须设置，不设置不保证顺序执行，默认为0,
     * 业务优先级约定从0开始，0表示顶级，不进行优先级校验，数字越小，优先级越高
     * 0优先与1执行，1优先于2执行，2优先于3执行
     * cn.yzw.jc2.common.retry.enums.BizSequencePriorityConstant
     */
    private Integer bizSequencePriority;

    /**
     * 允许执行时间范围
     * 上一个优先级的业务是非执行成功的状态或者不存在，那么当前业务创建后，允许多久时间后可执行
     * 时间单位为秒
     * 如果不设置，上一个业务不存在或者没执行成功，当前业务不允许执行
     */
    private Long    bizSequenceCanExecSecondTime;

    /**
     * 优先级高的任务不存在，优先级低的任务是否允许执行，例如bizSequencePriority=2，但是不存在<2的任务，2是否可以执行
     * NO表示不可以执行，YES表示可以执行，默认为YES,可以执行
     * cn.yzw.jc2.common.retry.enums.RetryTaskPriorityCheckEnums
     */
    private String  bizSequencePrevCheck;
}