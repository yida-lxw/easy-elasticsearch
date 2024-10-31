package cn.yzw.jc2.common.transfer.job;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;

import cn.hutool.core.util.IdUtil;
import cn.yzw.infra.component.utils.AssertUtils;
import cn.yzw.infra.component.utils.JsonUtils;
import cn.yzw.jc2.common.transfer.config.DTransferConfig;
import cn.yzw.jc2.common.transfer.enums.VerifyTypeEnum;
import cn.yzw.jc2.common.transfer.enums.WriteTypeEnum;
import cn.yzw.jc2.common.transfer.model.DTransferDoubleWriteProperties;
import cn.yzw.jc2.common.transfer.model.DTransferJobRequest;
import cn.yzw.jc2.common.transfer.model.DTransferVerifyJobRequest;
import cn.yzw.jc2.common.transfer.service.DTransferService;
import cn.yzw.jc2.common.transfer.service.DataVerifyService;
import cn.yzw.jc2.common.transfer.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 数据迁移job
 * @Author: lbl
 * @Date: 2024/10/17
 **/
@Slf4j(topic = "dtransfer")
public class DTransferJob {

    @Resource
    private DTransferService dTransferService;
    @Resource
    private DataVerifyService dataVerifyService;
    @Resource
    private DTransferConfig   dTransferConfig;
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
            //初始化数据源jdbcTemplate
            JdbcTemplate jdbcTemplate = CommonUtils.initJdbcTemplate(request.getDataSourceName());
            dTransferService.execute(request);
            log.info("本次任务id为{}表{}迁移任务执行耗时{}", request.getJobId(), request.getSourceTable(),
                System.currentTimeMillis() - startTime);
            return new ReturnT<>(ReturnT.SUCCESS_CODE, "执行完成，任务id为" + request.getJobId());
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
        ReturnT res = ReturnT.SUCCESS;
        try {
            DTransferVerifyJobRequest request = JsonUtils.readAsObject(paramStr, DTransferVerifyJobRequest.class);
            AssertUtils.notNull(request, "入参不能为空");
            AssertUtils.notBlank(request.getOldTable(), "老表名不能为空");
            AssertUtils.notNull(request.getNewTable(), "新表参数不能为空");
            AssertUtils.notBlank(request.getNewTable().getNewTableLogicName(), "新表逻辑表名不能为空");
            AssertUtils.notBlank(request.getNewTable().getNewTableRealNamePrefix(), "新表真实表名前缀不能为空");
            AssertUtils.notNull(request.getNewTable().getNewTableShardNum(), "新表分片数不能为空");
            AssertUtils.notBlank(request.getShardingKeyName(), "分片键不能为空");
            AssertUtils.notBlank(request.getPrimaryKeyName(), "唯一主键不能为空");
            if (request.getOldTableStartId() == null) {
                request.setOldTableStartId(Long.MIN_VALUE);
            }
            if (StringUtils.isBlank(request.getVerifyType())) {
                request.setVerifyType(VerifyTypeEnum.COMPARE_ALL.name());
            }
            if (CollectionUtils.isEmpty(request.getColumns()) && CollectionUtils.isEmpty(request.getIgnoreColumns())) {
                Set<String> ignoreColumns = new HashSet<>();
                ignoreColumns.add("id");
                request.setIgnoreColumns(ignoreColumns);
            }
            String oldTable = StringUtils.lowerCase(request.getOldTable());
            Map<String, DTransferDoubleWriteProperties> writePropertiesMap = dTransferConfig
                .getDoubleWritePropertiesMap();

            if (MapUtils.isEmpty(writePropertiesMap)) {
                log.warn("表{}不在双写阶段，不允许核对", oldTable);
                return res;
            }

            if (!writePropertiesMap.containsKey(oldTable)
                || !Boolean.TRUE.equals(writePropertiesMap.get(oldTable).getOpen())) {
                log.warn("表{}不在双写阶段，不允许核对", oldTable);
                return res;
            }
            DTransferDoubleWriteProperties dTransferDoubleWriteProperties = writePropertiesMap.get(oldTable);
            if (!WriteTypeEnum.WRITE_ALL_TABLE.name().equalsIgnoreCase(dTransferDoubleWriteProperties.getWriteType())) {
                log.warn("表{}不在双写阶段，不允许核对", oldTable);
                return res;
            }
            //初始化数据源jdbcTemplate
            JdbcTemplate jdbcTemplate = CommonUtils.initJdbcTemplate(request.getDataSourceName());
            dataVerifyService.verifyData(request,jdbcTemplate);
        } catch (Exception ex) {
            log.error("定时任务checkDataJob执行时出现异常", ex);
            res = ReturnT.FAIL;
            res.setMsg("处理时出现异常. " + TraceContext.traceId() + " " + ex.getMessage());
            XxlJobLogger.log(ex);
        }
        return res;
    }


}
