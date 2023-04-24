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

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RetryTaskJob {

    @Autowired
    private RetryTaskConfig        retryTaskConfig;
    @Autowired
    private RetryTaskDomainService supRetryTaskDomainService;
    @Resource
    private RetryTaskMapper        retryTaskMapper;

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
        while (true) {
            //1.批量查询
            List<Long> ids = retryTaskMapper.queryBatchForDel(500, minId, date, retryTaskConfig.getTableName());
            if (CollectionUtils.isNotEmpty(ids)) {
                retryTaskMapper.batchDeleteByPrimaryKey(ids, retryTaskConfig.getTableName());
                //设置下批查询起始id
                minId = ids.get(ids.size() - 1);
                num += ids.size();
            }
            if (CollectionUtils.isEmpty(ids) || ids.size() < 500) {
                log.info("retryTaskCleanJob执行结束,minId={}", minId);
                break;
            }

        }
        return new ReturnT(ReturnT.SUCCESS_CODE, String.format("删除成功%d条数据", num));
    }

    private void queryAndExecRetryTask() {
        String retryBatchNo = String.valueOf(System.currentTimeMillis());
        while (true) {
            List<RetryTaskDO> taskList = supRetryTaskDomainService.selectExecutableTask(
                retryTaskConfig.getTimeOutStartTime(), retryTaskConfig.getRetryTaskMaxRetryTimes(),
                retryTaskConfig.getRetryTaskPageSize(), retryBatchNo);
            if (CollectionUtils.isEmpty(taskList)) {
                break;
            }
            supRetryTaskDomainService.markRetryBatchNoByTaskNos(
                taskList.stream().map(RetryTaskDO::getRetryTaskNo).collect(Collectors.toList()), retryBatchNo);
            for (RetryTaskDO task : taskList) {
                supRetryTaskDomainService.asynExecTask(task.getRetryTaskNo());
            }
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
