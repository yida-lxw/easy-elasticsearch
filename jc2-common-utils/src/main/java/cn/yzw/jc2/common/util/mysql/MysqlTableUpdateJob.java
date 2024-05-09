package cn.yzw.jc2.common.util.mysql;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;

import cn.yzw.jc2.common.util.json.jackson.Jc2JacksonUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MysqlTableUpdateJob {

    @Autowired
    private MysqlTableFieldsUpdateExecute mysqlTableFieldsUpdateExecute;

    @XxlJob("mysqlTableUpdateJob")
    public ReturnT retryTaskExec(String paramStr) {
        try {
            UpdateTableParams param = null;
            if (StringUtils.isNotBlank(paramStr)) {
                param = Jc2JacksonUtil.parseObject(paramStr, UpdateTableParams.class);
            }
            if (param == null) {
                throw new RuntimeException("入参不能为空");
            }
            if (StringUtils.isBlank(param.getTableName())) {
                throw new RuntimeException("表名不能为空");
            }
            if (CollectionUtils.isEmpty(param.getFieldList())) {
                throw new RuntimeException("字段不能为空");
            }
            for (UpdateTableParams.UpdateField updateField : param.getFieldList()) {
                if (StringUtils.isBlank(updateField.getTargetFieldName())) {
                    throw new RuntimeException("目标字段不能为空");
                }
                if (UpdateFieldTypeEnum.FIELD.name().equals(updateField.getUpdateFieldType())) {
                    if (StringUtils.isBlank(updateField.getOriginFieldName())) {
                        throw new RuntimeException("原字段不能为空");
                    }
                } else {
                    if (updateField.getTargetValue() == null) {
                        throw new RuntimeException("目标字段值不能为空");
                    }
                }

            }
            mysqlTableFieldsUpdateExecute.execute(param);
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("重试任务-重试任务JOB-执行异常", e);
            return ReturnT.FAIL;
        }
    }

}
