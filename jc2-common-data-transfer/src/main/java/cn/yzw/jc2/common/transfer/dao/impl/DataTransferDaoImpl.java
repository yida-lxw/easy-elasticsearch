package cn.yzw.jc2.common.transfer.dao.impl;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;

import cn.yzw.jc2.common.transfer.common.AbstractDataTransferBaseService;
import cn.yzw.jc2.common.transfer.dao.DataTransferDao;
import cn.yzw.jc2.common.transfer.model.DataBaseTypeEnum;
import cn.yzw.jc2.common.transfer.model.ReadRequest;
import cn.yzw.jc2.common.transfer.model.WriteRequest;
import cn.yzw.jc2.common.transfer.utils.CommonRdbmsUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: yangle
 * @date: 2024/10/21
 **/
@Slf4j(topic = "dtransfer")
public class DataTransferDaoImpl extends AbstractDataTransferBaseService implements DataTransferDao {

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
