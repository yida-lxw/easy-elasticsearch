package cn.yzw.jc2.common.transfer.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.yzw.infra.component.utils.AssertUtils;
import cn.yzw.infra.component.utils.SpringContextUtils;
import cn.yzw.jc2.common.transfer.model.DataBaseTypeEnum;
import cn.yzw.jc2.common.transfer.model.ReadRequest;
import cn.yzw.jc2.common.transfer.model.WriteRequest;
import cn.yzw.jc2.common.transfer.service.DataTransferService;
import cn.yzw.jc2.common.transfer.utils.CommonRdbmsUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: yangle
 * @date: 2024/10/21
 **/
@Slf4j
public class DataTransferServiceImpl implements DataTransferService {

    private JdbcTemplate jdbcTemplate;

    private void initJdbcTemplate(String dataSourceName) {
        if (jdbcTemplate != null) {
            return;
        }
        synchronized (this) {
            if (jdbcTemplate != null) {
                return;
            }
            String[] beanNames = SpringContextUtils.getApplicationContext().getBeanNamesForType(DataSource.class);
            AssertUtils.notEmpty(beanNames, "未获取到数据源");
            if (beanNames.length == 1) {
                this.jdbcTemplate = new JdbcTemplate(SpringContextUtils.getBean(beanNames[0]));
            } else {
                AssertUtils.notBlank(dataSourceName, "数据源名称不能为空");
                AssertUtils.isTrue(Arrays.stream(beanNames).anyMatch(e -> dataSourceName.equals(e)),
                    "未匹配到数据源");
                this.jdbcTemplate = new JdbcTemplate(SpringContextUtils.getBean(dataSourceName));
            }
        }

    }

    @Override
    public List<Map<String, Object>> getDataList(ReadRequest request) {
        initJdbcTemplate(request.getDataSourceName());
        // 查询数据
        String sql = CommonRdbmsUtil.buildSqlLimit(DataBaseTypeEnum.valueOf(request.getDatasourceType()),
            request.getTable(), request.getQuerySql(), request.getStartId(), request.getEndId(), request.getIdList(),
            request.getLimit());
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public Long getMaxId(String table, String dataSourceName) {
        initJdbcTemplate(dataSourceName);
        String sql = "select max(id) from " + table;
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    @Override
    public void doBatchInsert(WriteRequest writeRequest) {
        try {
            initJdbcTemplate(writeRequest.getDataSourceName());
            int[] ints = jdbcTemplate.batchUpdate(writeRequest.getWriteTemplate(), writeRequest.getParams());
            log.info("任务id为{}批量写入成功,需写入条数为{},成功条数为{}", writeRequest.getJobId(), writeRequest.getParams().size(),
                ints.length);
        } catch (DataAccessException e) {
            log.error("任务id为{}批量写入失败，转换为单条执行，原因为", writeRequest.getJobId(), e);
            doOneInsert(writeRequest);
        } catch (Exception e) {
            log.error("任务id为{}批量写入失败，执行sql模版{}，执行参数{}，原因为", writeRequest.getJobId(), writeRequest.getWriteTemplate(),
                writeRequest.getParams(), e);
        }
    }

    @Override
    public void doOneInsert(WriteRequest writeRequest) {
        initJdbcTemplate(writeRequest.getDataSourceName());
        for (Object[] param : writeRequest.getParams()) {
            try {
                jdbcTemplate.update(writeRequest.getWriteTemplate(), param);
            } catch (Exception e) {
                log.error("任务id为{}写入失败，执行sql模版{}，执行参数{}，原因为", writeRequest.getJobId(), writeRequest.getWriteTemplate(),
                    param, e);
            }
        }
    }
}
