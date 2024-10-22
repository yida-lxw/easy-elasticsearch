package cn.yzw.jc2.common.transfer.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

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
@Service
@Slf4j
public class DataTransferServiceImpl implements DataTransferService {

    @Resource(name = "transferReadJdbcTemplate")
    private JdbcTemplate                 transferReadJdbcTemplate;

    @Resource(name = "transferWriteJdbcTemplate")
    private JdbcTemplate                 transferWriteJdbcTemplate;

    @Resource(name = "transferWriteTransactionManager")
    private DataSourceTransactionManager transferWriteTransactionManager;

    @Override
    public List<Map<String, Object>> getDataList(ReadRequest request) {
        // 查询数据
        String sql = CommonRdbmsUtil.buildSqlLimit(DataBaseTypeEnum.valueOf(request.getDatasourceType()),
            request.getTable(), request.getQuerySql(), request.getStartId(), request.getEndId(), request.getIdList(),
            request.getLimit());
        return transferReadJdbcTemplate.queryForList(sql);
    }

    @Override
    public Long getMaxId(String table) {
        String sql = "select max(id) from " + table;
        return transferReadJdbcTemplate.queryForObject(sql, Long.class);
    }

    @Override
    public void doBatchInsert(WriteRequest writeRequest) {
        TransactionStatus transactionStatus = null;
        try {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            transactionStatus = transferWriteTransactionManager.getTransaction(def);
            int[] ints = transferWriteJdbcTemplate.batchUpdate(writeRequest.getWriteTemplate(),
                writeRequest.getParams());
            transferWriteTransactionManager.commit(transactionStatus);
            log.info("任务id为{}批量写入成功,需写入条数为{},成功条数为{}", writeRequest.getJobId(), writeRequest.getParams().size(),
                ints.length);
        } catch (DataAccessException e) {
            if (transactionStatus != null) {
                transferWriteTransactionManager.rollback(transactionStatus);
            }
            log.error("任务id为{}批量写入失败，转换为单条执行，原因为", writeRequest.getJobId(), e);
            doOneInsert(writeRequest);
        } catch (Exception e) {
            log.error("任务id为{}批量写入失败，执行sql模版{}，执行参数{}，原因为", writeRequest.getJobId(), writeRequest.getWriteTemplate(),
                writeRequest.getParams(), e);
        }
    }

    @Override
    public void doOneInsert(WriteRequest writeRequest) {
        for (Object[] param : writeRequest.getParams()) {
            try {
                transferWriteJdbcTemplate.update(writeRequest.getWriteTemplate(), param);
            } catch (Exception e) {
                log.error("任务id为{}写入失败，执行sql模版{}，执行参数{}，原因为", writeRequest.getJobId(), writeRequest.getWriteTemplate(),
                    param, e);
            }
        }
    }
}
