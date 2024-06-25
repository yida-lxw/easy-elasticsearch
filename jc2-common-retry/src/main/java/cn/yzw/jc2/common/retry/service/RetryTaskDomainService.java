package cn.yzw.jc2.common.retry.service;


import cn.yzw.jc2.common.retry.entity.RetryCreateTask;
import cn.yzw.jc2.common.retry.entity.RetryTaskBizMethodHolder;
import cn.yzw.jc2.common.retry.entity.RetryTaskDO;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public interface RetryTaskDomainService {

    /**
     * 创建重试任务，重试任务如果设置了优先级，优先级最好从0开始
     * @param retryTaskNo 重试任务唯一编号
     * @param bizKey 业务key,用于标注业务类型以及查找执行方法
     * @param bizUniqueKey 业务唯一key或者编号，可空，为空时任务无序并行执行，非空时唯一key相同的任务根据ID顺序串行执行
     * @param taskData 任务执行参数数据
     * @param planExecTime 任务期望执行时间，可空，为空时期望执行时间为当前时间，则尽快执行
     * @return 重试任务编号
     */
    String createTask(RetryCreateTask createTask);

    /**
     * 批量创建创建重试任务，重试任务如果设置了优先级，优先级最好从0开始
     * @return 重试任务编号
     */
    List<String> batchCreateTask(List<RetryCreateTask> createTaskList);

    /**
     * 同步执行重试任务
     * 重要!!! syncExecTask不能和createTask放在一个事务里,创建任务事务提交后,才能执行任务
     * 如果任务的biz_unique_key不为空，则会将具有相同的biz_unique_key的任务都查询出来，根据任务ID串行执行
     * @param taskNo 重试任务编号
     */
    void syncExecTask(String taskNo);

    /**
     * 异步执行重试任务
     * 重要!!! asynExecTask不能和createTask放在一个事务里,创建任务事务提交后,才能执行任务
     * 如果任务的biz_unique_key不为空，则会将具有相同的biz_unique_key的任务都查询出来，根据任务ID串行执行
     * @param taskNo 重试任务编号
     */
    void asynExecTask(String taskNo);

    /**
     * 查询可执行的重试任务列表
     * @param timeOutStartTime 超时的执行开始时间
     * @param maxRetryTimes 最大重试次数
     * @param pageSize 页长
     * @param retryBatchNo 执行批次号
     * @return
     */
    List<RetryTaskDO> selectExecutableTask(Date timeOutStartTime, Integer maxRetryTimes, Integer pageSize,
                                           Long minId, List<String> includeBizTypes, List<String> excludeBizTypes);

    /**
     * 查询不可执行的重试任务列表
     * 状态为FAIL，并且执行次数大于等于最大重试次数
     * @param maxRetryTimes 最大重试次数
     * @param pageSize 页长
     * @return
     */
    List<RetryTaskDO> selectUnexecutableTask(Integer maxRetryTimes, Integer pageSize);

    /**
     * 根据任务编号删除重试任务
     * @param retryTaskNo 任务编号
     * @return
     */
    int deleteByNo(String retryTaskNo);

    /**
     * 根据任务编号查询重试任务
     * @param retryTaskNo 任务编号
     * @return
     */
    RetryTaskDO selectByNo(String retryTaskNo);

    /**
     * 根据任务编号重置：`task_exec_count` '任务执行次数'
     * @param retryTaskNo 任务编号
     * @return
     */
    int resetByNo(String retryTaskNo);

    /**
     * 根据任务编号批量查询重试任务
     * @param retryTaskNos 任务编号
     * @return
     */
    List<RetryTaskDO> batchQueryByTaskNos(List<String> retryTaskNos);

    ConcurrentMap<String, RetryTaskBizMethodHolder> getBizMethodRegistry();
}
