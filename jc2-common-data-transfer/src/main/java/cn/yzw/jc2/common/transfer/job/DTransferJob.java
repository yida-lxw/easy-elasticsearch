package cn.yzw.jc2.common.transfer.job;

import java.util.Objects;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.context.ApplicationContext;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;

import cn.hutool.core.util.IdUtil;
import cn.yzw.infra.component.utils.JsonUtils;
import cn.yzw.jc2.common.transfer.factory.DTransferFactory;
import cn.yzw.jc2.common.transfer.model.DTransferJobRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 数据迁移job
 * @Author: lbl
 * @Date: 2024/10/17
 **/
@Slf4j
public class DTransferJob {

    @Resource
    private DTransferFactory   dTransferFactory;

    private ApplicationContext applicationContext;

    /**
     * @Description: 全量数据迁移job
     * @Author: lbl
     * @Date:  2024/10/17 14:49
     * @param:
     * @return:
     **/
    @XxlJob("fullDTransferJob")
    public ReturnT fullDTransferJob(String params) {
        XxlJobLogger.log(TraceContext.traceId());
        DTransferJobRequest request = JsonUtils.readAsObject(params, DTransferJobRequest.class);
        if (Objects.isNull(request)) {
            return new ReturnT(ReturnT.FAIL_CODE, "fullDTransferJob任务入参不能为空！！！");
        }
        if (StringUtils.isBlank(request.getSourceTable())) {
            return new ReturnT(ReturnT.FAIL_CODE, "fullDTransferJob任务源表名不能为空！！！");
        }
        if (StringUtils.isBlank(request.getTargetTable())) {
            return new ReturnT(ReturnT.FAIL_CODE, "fullDTransferJob任务目标表名不能为空！！！");
        }
        request.setJobId(Objects.nonNull(request.getJobId()) ? request.getJobId() : IdUtil.simpleUUID());
        log.info("本次表{}迁移任务开始,目标表{}，参数为{}，json解析后{}，本次任务id为{}", request.getSourceTable(), request.getTargetTable(),
            params, request, request.getJobId());
        try {
            long startTime = System.currentTimeMillis();
            dTransferFactory.execute(request);
            log.info("本次任务id为{}-表{}迁移任务执行耗时{}", request.getJobId(), request.getSourceTable(),
                System.currentTimeMillis() - startTime);
            return new ReturnT<>(ReturnT.SUCCESS_CODE, "执行完成，任务id" + request.getJobId());
        } catch (Exception ex) {
            log.error("定时任务fullDTransferJob执行时出现异常,任务id为{}", request.getJobId(), ex);
            XxlJobLogger.log(ex);
            return ReturnT.FAIL;
        }
    }

    /**
     * @Description: 数据核对job
     * @Author: lbl
     * @Date:  2024/10/17 14:49
     * @param: 基于老表对新表的字段一致性进行比较，基于新表查询数据在老表是否存在，不存在则删除
     * @return:
     **/
    @XxlJob("checkDataJob")
    public ReturnT checkDataJob(String paramStr) {
        XxlJobLogger.log(TraceContext.traceId());
        log.info("params: {}", paramStr);
        ReturnT res = new ReturnT();

        try {

        } catch (Exception ex) {
            log.error("定时任务checkDataJob执行时出现异常", ex);
            res = ReturnT.FAIL;
            res.setMsg("处理时出现异常. " + TraceContext.traceId() + " " + ex.getMessage());
            XxlJobLogger.log(ex);
        }
        return res;
    }
}
