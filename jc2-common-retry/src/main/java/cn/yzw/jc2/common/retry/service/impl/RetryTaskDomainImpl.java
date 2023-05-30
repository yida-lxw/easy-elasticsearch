package cn.yzw.jc2.common.retry.service.impl;

import cn.yzw.infra.component.utils.AssertUtils;
import cn.yzw.jc2.common.retry.config.RetryTaskConfig;
import cn.yzw.jc2.common.retry.entity.RetryCreateTask;
import cn.yzw.jc2.common.retry.entity.RetryTaskDO;
import cn.yzw.jc2.common.retry.enums.RetryTaskPriorityCheckEnums;
import cn.yzw.jc2.common.retry.enums.RetryTaskStatusEnum;
import cn.yzw.jc2.common.retry.mapper.RetryTaskMapper;
import cn.yzw.jc2.common.retry.annotation.RetryTask;
import cn.yzw.jc2.common.retry.entity.RetryTaskBizMethodHolder;
import cn.yzw.jc2.common.retry.service.RetryTaskDomainService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class RetryTaskDomainImpl implements RetryTaskDomainService, ApplicationContextAware,
                                 SmartInitializingSingleton {
    private final static String retry_task_EXEC_LOCK_PREFIX = "retry_task_exec_lock_";

    @Resource
    private RetryTaskMapper     retryTaskMapper;

    @Autowired
    private RedissonClient      redissonClient;

    @Autowired
    @Qualifier("retryTaskThreadPoolTaskExecutor")
    private ThreadPoolExecutor  retryTaskThreadPoolTaskExecutor;

    @Autowired
    private RetryTaskConfig     retryTaskConfig;

    @Override
    public String createTask(RetryCreateTask createTask) {
        RetryTaskDO retryTaskDO = buildTaskDO(createTask);
        retryTaskMapper.insert(retryTaskDO);
        return retryTaskDO.getRetryTaskNo();
    }

    @Override
    public List<String> batchCreateTask(List<RetryCreateTask> createTaskList) {
        List<RetryTaskDO> saveList = new ArrayList<>(createTaskList.size());
        createTaskList.forEach(createTask -> {
            RetryTaskDO retryTaskDO = buildTaskDO(createTask);
            saveList.add(retryTaskDO);
        });
        retryTaskMapper.batchInsert(saveList, retryTaskConfig.getTableName());
        return saveList.stream().map(RetryTaskDO::getRetryTaskNo).collect(Collectors.toList());
    }

    private RetryTaskDO buildTaskDO(RetryCreateTask createTask) {
        AssertUtils.notBlank(createTask.getRetryTaskNo(), "重试任务唯一编号不能为空");
        AssertUtils.notBlank(createTask.getBizKey(), "业务key不能为空");
        AssertUtils.notBlank(createTask.getTaskData(), "重试任务数据不能为空");
        RetryTaskDO retryTaskDO = RetryTaskDO.builder().retryTaskNo(createTask.getRetryTaskNo())
            .bizKey(createTask.getBizKey()).bizSequenceNo(createTask.getBizSequenceNo())
            .bizSequencePriority(createTask.getBizSequencePriority() == null ? 0 : createTask.getBizSequencePriority())
            .bizSequenceCanExecSecondTime(createTask.getBizSequenceCanExecSecondTime())
            .bizSequenceLowPriorityCanExec(
                RetryTaskPriorityCheckEnums.NO.name().equals(createTask.getBizSequenceLowPriorityCanExec())
                    ? RetryTaskPriorityCheckEnums.NO.name()
                    : RetryTaskPriorityCheckEnums.YES.name())
            .taskPlanExecTime(createTask.getTaskPlanExecTime() != null ? createTask.getTaskPlanExecTime() : new Date())
            .taskExecStatus(RetryTaskStatusEnum.PENDING.name()).taskExecCount(0).taskData(createTask.getTaskData())
            .tableName(retryTaskConfig.getTableName()).build();
        return retryTaskDO;
    }

    @Override
    public void syncExecTask(String taskNo) {
        this.execTask(taskNo);
    }

    @Override
    public void asynExecTask(String taskNo) {
        retryTaskThreadPoolTaskExecutor.execute(() -> this.execTask(taskNo));
    }

    @Override
    public List<RetryTaskDO> selectExecutableTask(Date timeOutStartTime, Integer maxRetryTimes, Integer pageSize,
                                                  Long minId) {
        return retryTaskMapper.selectExecutableTask(timeOutStartTime, maxRetryTimes, pageSize, minId,
            retryTaskConfig.getTableName());
    }



    @Override
    public int deleteByNo(String retryTaskNo) {
        if (StringUtils.isEmpty(retryTaskNo)) {
            return 0;
        }
        return retryTaskMapper.deleteByNo(retryTaskNo, retryTaskConfig.getTableName());
    }

    @Override
    public int resetByNo(String retryTaskNo) {
        if (StringUtils.isEmpty(retryTaskNo)) {
            return 0;
        }
        return retryTaskMapper.resetByNo(retryTaskNo, retryTaskConfig.getTableName());
    }

    @Override
    public List<RetryTaskDO> selectUnexecutableTask(Integer maxRetryTimes, Integer pageSize) {
        return retryTaskMapper.selectUnexecutableTask(maxRetryTimes, pageSize, retryTaskConfig.getTableName());
    }

    @Override
    public RetryTaskDO selectByNo(String retryTaskNo) {
        if (StringUtils.isEmpty(retryTaskNo)) {
            return null;
        }
        return retryTaskMapper.selectByNo(retryTaskNo, retryTaskConfig.getTableName());
    }

    private void execTask(String taskNo) {
        if (StringUtils.isBlank(taskNo)) {
            return;
        }
        RetryTaskDO taskDO = retryTaskMapper.selectByNo(taskNo, retryTaskConfig.getTableName());
        if (taskDO == null) {
            return;
        }
        String bizSequenceNo = taskDO.getBizSequenceNo();
        if (StringUtils.isBlank(bizSequenceNo)) {
            //不需要保证执行顺序
            this.execTask(taskDO.getRetryTaskNo(), taskDO.getBizKey());
        } else {
            RLock lock = null;
            boolean lockSuccess = false;
            try {
                //有顺序执行
                lock = redissonClient.getLock(buildExecLockKey(bizSequenceNo));
                lockSuccess = lock.tryLock(0L, 600 * 1000L, TimeUnit.MILLISECONDS);
                if (lockSuccess) {
                    List<RetryTaskDO> retryTaskDOList = retryTaskMapper.selectExecTaskByBizSequenceNo(
                        retryTaskConfig.getTimeOutStartTime(), retryTaskConfig.getRetryTaskMaxRetryTimes(),
                        bizSequenceNo, retryTaskConfig.getTableName());
                    for (RetryTaskDO retryTaskDO : retryTaskDOList) {
                        boolean execSuccess = this.execTask(retryTaskDO.getRetryTaskNo(), retryTaskDO.getBizKey());
                        if (!execSuccess) {
                            break;
                        }
                    }
                } else {
                    log.info("重试任务-跳过执行-未获取到执行锁，bizSequenceNo：{}", bizSequenceNo);
                }
            } catch (Exception e) {
                log.warn("重试任务-执行异常，bizSequenceNo：{}", bizSequenceNo, e);
            } finally {
                if (lock != null && lockSuccess) {
                    lock.unlock();
                }
            }
        }
    }

    private boolean execTask(String taskNo, String bizKey) {
        if (StringUtils.isBlank(taskNo) || StringUtils.isBlank(bizKey)) {
            return false;
        }
        RLock lock = null;
        boolean lockSuccess = false;
        RetryTaskDO taskDO = null;
        try {
            RetryTaskBizMethodHolder methodHolder = bizMethodRegistry.get(bizKey);
            if (methodHolder == null) {
                throw new RuntimeException("未找到bizKey对应的业务方法,key=" + bizKey);
            }
            lock = redissonClient.getLock(buildExecLockKey(taskNo));
            lockSuccess = lock.tryLock(0L, methodHolder.getLockSeconds() * 1000L, TimeUnit.MILLISECONDS);
            if (lockSuccess) {
                taskDO = retryTaskMapper.selectByNo(taskNo, retryTaskConfig.getTableName());
                if (taskDO != null) {
                    if (RetryTaskStatusEnum.SUCCESS.name().equals(taskDO.getTaskExecStatus())) {
                        log.info("重试任务-已经执行成功-本次放弃执行！task：{}", taskDO);
                        return true;
                    }
                    log.info("重试任务-开始执行，retry_task_no：{}", taskNo);
                    AssertUtils.isTrue(validatePrevBizSequenceTask(bizKey, taskDO), "上一优先级的任务未执行成功，本级业务不允许执行");
                    retryTaskMapper.updateExecutingStatusByNo(taskNo, RetryTaskStatusEnum.EXECUTING.name(),
                        retryTaskConfig.getTableName());
                    methodHolder.getBizMethod().invoke(methodHolder.getTargetService(), taskDO.getTaskData());
                    retryTaskMapper.updateResultStatusByNo(taskNo, RetryTaskStatusEnum.SUCCESS.name(), null,
                        retryTaskConfig.getTableName());
                    log.info("重试任务-成功-执行完成任务！task：{}", taskDO);
                }
                return true;
            } else {
                log.info("重试任务-跳过执行-未获取到执行锁，retry_task_no：{}", taskNo);
                return false;
            }
        } catch (Exception e) {
            log.warn("重试任务-执行异常，retry_task_no：{}, task：{}", taskNo, taskDO, e);
            String msg = e.getMessage();
            if (e instanceof InvocationTargetException) {
                msg = ((InvocationTargetException) e).getTargetException().getMessage();
            }
            if (msg != null && msg.length() > 500) {
                msg = msg.substring(0, 500);
            }
            if (taskDO != null) {
                retryTaskMapper.updateResultStatusByNo(taskNo, RetryTaskStatusEnum.FAIL.name(),
                    StringUtils.isBlank(msg) ? "空指针了！！！" : msg,
                    retryTaskConfig.getTableName());
            }
            return false;
        } finally {
            if (lock != null && lockSuccess) {
                lock.unlock();
            }
        }
    }

    /**
     * 上一个优先级的业务是非执行成功的状态或者不存在，那么当前业务创建后，允许多久时间后可执行
     * 时间单位为秒
     * 如果不设置，上一个业务没执行成功，当前业务不允许执行
     */
    private boolean validatePrevBizSequenceTask(String bizKey, RetryTaskDO taskDO) {
        //0为顶级不校验
        if (taskDO.getBizSequenceNo() != null && taskDO.getBizSequencePriority() != null
            && taskDO.getBizSequencePriority() > 0) {
            List<RetryTaskDO> preRetryTaskDOList = retryTaskMapper.selectPrevBizSequenceTask(taskDO.getBizSequenceNo(),
                bizKey, taskDO.getBizSequencePriority(), retryTaskConfig.getTableName());
            if (CollectionUtils.isEmpty(preRetryTaskDOList)) {
                return RetryTaskPriorityCheckEnums.YES.name().equals(taskDO.getBizSequenceLowPriorityCanExec());
            } else {
                List<RetryTaskDO> taskDOList = preRetryTaskDOList.stream()
                    .filter(e -> !e.getTaskExecStatus().equals(RetryTaskStatusEnum.SUCCESS.name()))
                    .collect(Collectors.toList());
                //存在优先级高的任务没执行成功，优先级低的任务不允许执行
                return CollectionUtils.isEmpty(taskDOList);
            }
        } else {
            return true;
        }

    }

    private String buildExecLockKey(String taskNo) {
        return retryTaskConfig.getAppName() + "_" + retry_task_EXEC_LOCK_PREFIX + taskNo;
    }

    @Override
    public void afterSingletonsInstantiated() {
        initBizMethodRegistry(this.applicationContext);
    }

    private final ConcurrentMap<String, RetryTaskBizMethodHolder> bizMethodRegistry = new ConcurrentHashMap<>();

    private void initBizMethodRegistry(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            Map<Method, RetryTask> annotatedMethods = null;
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                    (MethodIntrospector.MetadataLookup<RetryTask>) method -> AnnotatedElementUtils
                        .findMergedAnnotation(method, RetryTask.class));
            } catch (Throwable ex) {
                log.error("retryTask biz method resolve error for bean[" + beanDefinitionName + "].", ex);
            }
            if (annotatedMethods == null || annotatedMethods.isEmpty()) {
                continue;
            }
            for (Map.Entry<Method, RetryTask> methodRetryTaskEntry : annotatedMethods.entrySet()) {
                Method method = methodRetryTaskEntry.getKey();
                RetryTask retryTask = methodRetryTaskEntry.getValue();
                if (retryTask == null) {
                    continue;
                }
                String bizKey = retryTask.value();
                long lockSeconds = retryTask.lockSeconds();
                if (StringUtils.isBlank(bizKey)) {
                    throw new RuntimeException(
                        "retryTask biz method bizKey invalid, for[" + bean.getClass() + "#" + method.getName() + "] .");
                }
                if (this.bizMethodRegistry.containsKey(bizKey)) {
                    throw new RuntimeException("retryTask biz method[" + bizKey + "] naming conflicts.");
                }
                if (!(method.getParameterTypes().length == 1
                      && method.getParameterTypes()[0].isAssignableFrom(String.class))) {
                    throw new RuntimeException("retryTask biz method param-classtype invalid, for[" + bean.getClass()
                                               + "#" + method.getName() + "] , "
                                               + "The correct method format like \" public void bizMethod(String param) \" .");
                }
                method.setAccessible(true);
                bizMethodRegistry.put(bizKey, RetryTaskBizMethodHolder.builder().targetService(bean).bizMethod(method)
                    .lockSeconds(lockSeconds).build());
            }
        }
    }

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
