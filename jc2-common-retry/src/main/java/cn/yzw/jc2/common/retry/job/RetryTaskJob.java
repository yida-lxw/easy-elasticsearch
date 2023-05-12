package cn.yzw.jc2.common.retry.job;

import cn.yzw.infra.component.utils.DateUtils;
import cn.yzw.infra.component.utils.JsonUtils;
import cn.yzw.jc2.common.retry.config.RetryTaskConfig;
import cn.yzw.jc2.common.retry.entity.RetryTaskDO;
import cn.yzw.jc2.common.retry.mapper.RetryTaskMapper;
import cn.yzw.jc2.common.retry.service.RetryTaskDomainService;
import cn.yzw.jc2.common.util.log.LogUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class RetryTaskJob {

    @Autowired
    private RetryTaskConfig        retryTaskConfig;
    @Autowired
    private RetryTaskDomainService supRetryTaskDomainService;
    @Resource
    private RetryTaskMapper        retryTaskMapper;
    @Autowired
    @Qualifier("retryTaskThreadPoolTaskExecutor")
    private ThreadPoolExecutor     retryTaskThreadPoolTaskExecutor;

    @XxlJob("retryTaskExeJob")
    public ReturnT retryTaskExec(String paramStr) {
        try {
            LogUtil.setNewTraceIdFromJob();
            Param param = null;
            if (StringUtils.isNotBlank(paramStr)) {
                param = JsonUtils.readAsObject(paramStr, Param.class);
            }
            param = (param == null ? new Param() : param);
            if (Param.EXECUTE_TASK.equals(param.getMode())) {
                if (CollectionUtils.isNotEmpty(param.getExecuteTaskNos())) {
                    executeTaskByNos(param.getExecuteTaskNos());
                } else {
                    // 查询可执行的重试任务，并执行任务
                    queryAndExecRetryTask();
                }
                // 查询是否存在超过最大执行次数的任务，存在则打印警告日志
                queryAndLogWarnRetryTask();
            } else if (Param.DELETE_TASK.equals(param.getMode())) {
                // 删除重试任务
                deleteTaskByNos(param.getDeleteTaskNos());
            } else if (Param.RESET_TASK.equals(param.getMode())) {
                if (CollectionUtils.isNotEmpty(param.getResetTaskNos())) {
                    // 重置任务次数
                    resetTaskByNos(param.getResetTaskNos());
                }
            }
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("重试任务-重试任务JOB-执行异常", e);
            return ReturnT.FAIL;
        } finally {
            LogUtil.removeTraceID();
        }
    }

    /**
     * 清理执行成功的数据
     * @param param
     * @return
     */
    @XxlJob("retryTaskCleanJob")
    public ReturnT clean(String param) {
        LogUtil.setNewTraceIdFromJob();
        Long minId = null;
        int num = 0;
        Date date = DateUtils.addDays(new Date(), -1 * retryTaskConfig.getCleanBeforeDays());
        List ids;
        do {
            ids = this.retryTaskMapper.queryBatchForDel(500, minId, date, this.retryTaskConfig.getTableName());
            if (CollectionUtils.isNotEmpty(ids)) {
                this.retryTaskMapper.batchDeleteByPrimaryKey(ids, this.retryTaskConfig.getTableName());
                minId = (Long) ids.get(ids.size() - 1);
                num += ids.size();
            }
        } while (!CollectionUtils.isEmpty(ids) && ids.size() >= 500);
        return new ReturnT(ReturnT.SUCCESS_CODE, String.format("删除成功%d条数据", num));
    }

    private void queryAndExecRetryTask() {
        Long minId = Long.MIN_VALUE;
        //一次调度超过10000次，中断，等待xxljob下次调度
        int execCount = 10000;
        int i = 0;
        while (i++ < execCount) {
            if (retryTaskThreadPoolTaskExecutor.getQueue().size() > retryTaskConfig.getQueuePoolSize() / 2) {
                try {
                    Thread.sleep(200L);
                    continue;
                } catch (InterruptedException e) {
                    log.error("线程异常", e);
                }
            }
            List<RetryTaskDO> taskList = supRetryTaskDomainService.selectExecutableTask(
                retryTaskConfig.getTimeOutStartTime(), retryTaskConfig.getRetryTaskMaxRetryTimes(),
                retryTaskConfig.getRetryTaskPageSize(), minId);
            if (CollectionUtils.isEmpty(taskList)) {
                break;
            }
            for (RetryTaskDO task : taskList) {
                supRetryTaskDomainService.asynExecTask(task.getRetryTaskNo());
            }
            minId = taskList.get(taskList.size() - 1).getId();
        }
    }

    private void executeTaskByNos(List<String> retryTaskNos) {
        if (CollectionUtils.isNotEmpty(retryTaskNos)) {
            for (String retryTaskNo : retryTaskNos) {
                supRetryTaskDomainService.asynExecTask(retryTaskNo);
            }
        }
    }

    private void queryAndLogWarnRetryTask() {
        List<RetryTaskDO> taskList = supRetryTaskDomainService
            .selectUnexecutableTask(retryTaskConfig.getRetryTaskMaxRetryTimes(), 1);
        if (CollectionUtils.isNotEmpty(taskList)) {
            log.warn("重试任务需人工处理：存在超过最大执行次数的重试任务");
        }
    }

    private void deleteTaskByNos(List<String> deleteTaskNos) {
        if (CollectionUtils.isNotEmpty(deleteTaskNos)) {
            for (String deleteTaskNo : deleteTaskNos) {
                RetryTaskDO taskDO = supRetryTaskDomainService.selectByNo(deleteTaskNo);
                if (taskDO != null) {
                    supRetryTaskDomainService.deleteByNo(deleteTaskNo);
                    log.info("重试任务-删除-定时任务删除！task：{}", taskDO);
                }
            }
        }
    }

    private void resetTaskByNos(List<String> resetTaskNos) {
        if (CollectionUtils.isNotEmpty(resetTaskNos)) {
            for (String resetTaskNo : resetTaskNos) {
                RetryTaskDO taskDO = supRetryTaskDomainService.selectByNo(resetTaskNo);
                if (taskDO != null) {
                    taskDO.setTaskExecCount(0);
                    supRetryTaskDomainService.resetByNo(resetTaskNo);
                    log.info("重试任务-重置-重置定时任务次数！task：{}", taskDO);
                }
            }
        }
    }

    @Data
    private static class Param {
        public static final String EXECUTE_TASK = "EXECUTE_TASK";
        public static final String DELETE_TASK  = "DELETE_TASK";
        public static final String RESET_TASK   = "RESET_TASK";

        private String             mode;
        private List<String>       deleteTaskNos;
        private List<String>       executeTaskNos;
        private List<String>       resetTaskNos;

        public Param() {
            this.mode = Param.EXECUTE_TASK;
        }
    }
}
