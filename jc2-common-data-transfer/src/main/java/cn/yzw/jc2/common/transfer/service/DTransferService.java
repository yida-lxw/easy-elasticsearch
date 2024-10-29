package cn.yzw.jc2.common.transfer.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Resource;

import org.springframework.util.CollectionUtils;

import cn.hutool.core.collection.CollectionUtil;
import cn.yzw.jc2.common.transfer.dao.DataTransferDao;
import cn.yzw.jc2.common.transfer.model.DTransferJobRequest;
import cn.yzw.jc2.common.transfer.model.ReadRequest;
import cn.yzw.jc2.common.transfer.model.WriteRequest;
import cn.yzw.jc2.common.transfer.utils.CommonRdbmsUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 全量迁移
 *
 * @author yl
 */
@Slf4j
public class DTransferService {

    @Resource
    private DataTransferDao dataTransferService;

    public void execute(DTransferJobRequest request) {
        if (CollectionUtil.isNotEmpty(request.getIdList())) {
            request.setEndId(Collections.max(request.getIdList()));
        } else if (Objects.isNull(request.getEndId())){
            Long maxId = dataTransferService.getMaxId(request.getSourceTable(), request.getDataSourceName());
            request.setEndId(maxId);
        }
        if (request.getEndId() < request.getStartId()) {
            log.info("任务id为{}结束，因为开始id超过结束id", request.getJobId());
            return;
        }
        if (request.getThreadCount() != null && request.getThreadCount() > 1) {
            ExecutorService pool = null;
            try {
                AtomicLong loop = new AtomicLong(request.getStartId());
                int threadNum = Math.min(request.getThreadCount(), 10);
                pool = Executors.newFixedThreadPool(threadNum);
                List<Future> tasks = new ArrayList<>(threadNum);
                long startTime = System.currentTimeMillis();
                log.info("任务id{}分片执行开始，分片数量为{}", request.getJobId(), threadNum);
                for (int i = 0; i < threadNum; i++) {
                    tasks.add(pool.submit(() -> concurrentExecute(request, loop)));
                }
                tasks.forEach(e -> {
                    try {
                        e.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        log.error("data-sync-error: 数据迁移线程执行异常", ex);
                    }
                });
                log.info("任务id{}分片执行结束，分片数量为{}，花费时间为{}", request.getJobId(), threadNum,
                    System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("data-sync-error: 数据迁移线程执行异常", e);
            } finally {
                //销毁线程池
                if (Objects.nonNull(pool)) {
                    pool.shutdownNow();
                }
            }
        } else {
            log.info("任务id{}串行执行", request.getJobId());
            serialExecute(request);
        }

    }

    private void serialExecute(DTransferJobRequest request) {
        while (true) {
            try {
                //批量查询
                ReadRequest readRequest = new ReadRequest();
                readRequest.setTable(request.getSourceTable());
                readRequest.setStartId(request.getStartId());
                readRequest.setEndId(request.getEndId());
                readRequest.setIdList(request.getIdList());
                readRequest.setLimit(request.getLimit());
                readRequest.setQuerySql(request.getQuerySql());
                readRequest.setDatasourceType(request.getDatasourceType());
                readRequest.setDataSourceName(readRequest.getDataSourceName());
                List<Map<String, Object>> dataList = dataTransferService.getDataList(readRequest);

                //本批数据处理
                if (CollectionUtils.isEmpty(dataList)) {
                    break;
                }
                //本批数据处理
                this.execute(dataList, request);
            } catch (Exception e) {
                log.error("data-sync-error: 同步异常.");
            }
        }
    }

    private void concurrentExecute(DTransferJobRequest request, AtomicLong loop) {
        while (true) {
            //获取分片
            try {
                Long executorStartId = loop.getAndAdd(request.getLimit());
                Long executorEndId = loop.get();
                long startTime = System.currentTimeMillis();
                log.info("任务id为{}开始分段获取数据，获取到分段区间id为{}-{}", request.getJobId(), executorStartId, executorEndId);
                if (executorStartId > request.getEndId()) {
                    break;
                }
                ReadRequest readRequest = new ReadRequest();
                readRequest.setTable(request.getSourceTable());
                readRequest.setStartId(executorStartId);
                readRequest.setEndId(executorEndId);
                readRequest.setIdList(request.getIdList());
                readRequest.setLimit(request.getLimit());
                readRequest.setQuerySql(request.getQuerySql());
                readRequest.setDatasourceType(request.getDatasourceType());
                readRequest.setDataSourceName(readRequest.getDataSourceName());
                List<Map<String, Object>> dataList = dataTransferService.getDataList(readRequest);
                log.info("任务id为{}分段获取数据结束，获取到分段区间id为{}-{}，一共获取到数据量为：{}，查询花费时间为{}", request.getJobId(), executorStartId,
                    executorEndId, dataList.size(), System.currentTimeMillis() - startTime);
                //本批数据处理
                if (CollectionUtil.isNotEmpty(dataList)) {
                    long start = System.currentTimeMillis();
                    log.info("任务id为{}分段写数据开始，获取到分段区间id为{}-{}，一共写入的数据量为：{}", request.getJobId(), executorStartId,
                        executorEndId, dataList.size());
                    this.execute(dataList, request);
                    log.info("任务id为{}分段写数据开始，获取到分段区间id为{}-{}，一共写入的数据量为：{}，花费时间为{}", request.getJobId(), executorStartId,
                        executorEndId, dataList.size(), System.currentTimeMillis() - start);
                }
            } catch (Exception e) {
                log.error("data-sync-error: 同步异常.任务id为{}，表名为{}", request.getJobId(), request.getSourceTable(), e);
            }
        }
    }

    /**
     * @desc 按操作类型过滤处理数据，删除和增删分2个事务执行，确保增改失败，不影响删除执行成功
     * @author liangbaole
     * @date 2022/9/14
     */
    private void execute(List<Map<String, Object>> dataList, DTransferJobRequest request) {
        try {
            //同步忽略id
            if (Boolean.TRUE.equals(request.getIgnoreId())) {
                for (Map<String, Object> map : dataList) {
                    map.remove("id");
                }
            }
            //分库分表前期逻辑表手动拼_NEW，因为双写与老表区分
            WriteRequest writeRequest = new WriteRequest();
            writeRequest.setTargetTable(request.getTargetTable());
            writeRequest.setDataSourceName(request.getDataSourceName());
            List<String> columns = new ArrayList<>(dataList.get(0).keySet());
            List<String> valueHolders = new ArrayList<>(columns.size());
            for (int i = 0; i < columns.size(); i++) {
                valueHolders.add("?");
            }
            String writeTemplate = CommonRdbmsUtil.getWriteTemplate(columns, valueHolders, writeRequest.getTargetTable());
            List<Object[]> params = new ArrayList<>();
            for (Map<String, Object> map : dataList) {
                Object[] objects = map.values().toArray();
                params.add(objects);
            }
            writeRequest.setWriteTemplate(writeTemplate);
            writeRequest.setParams(params);
            writeRequest.setJobId(request.getJobId());
            dataTransferService.doBatchInsert(writeRequest);
        } catch (Exception e) {
            log.error("data-sync-error: 同步异常.任务id为{}，表名为{}，数据为{}", request.getJobId(), request.getSourceTable(), dataList, e);
        }
    }
}
