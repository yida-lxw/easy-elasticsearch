package cn.yzw.jc2.common.transfer.job;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;

/**
 * @Description: 数据迁移job
 * @Author: lbl
 * @Date: 2024/10/17
 **/
@Slf4j
public class DTransferJob {

    /**
     * @Description: 全量数据迁移job
     * @Author: lbl
     * @Date:  2024/10/17 14:49
     * @param:
     * @return:
     **/
    @XxlJob("fullDTransferJob")
    public ReturnT fullDTransferJob(String paramStr) {
        XxlJobLogger.log(TraceContext.traceId());
        log.info("params: {}", paramStr);
        ReturnT res = new ReturnT();

        try {

        } catch (Exception ex) {
            log.error("定时任务fullDTransferJob执行时出现异常", ex);
            res = ReturnT.FAIL;
            res.setMsg("处理时出现异常. " + TraceContext.traceId() + " " + ex.getMessage());
            XxlJobLogger.log(ex);
        }
        return res;
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
