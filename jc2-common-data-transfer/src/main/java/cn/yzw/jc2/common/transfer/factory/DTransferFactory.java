package cn.yzw.jc2.common.transfer.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import cn.hutool.core.collection.CollectionUtil;
import cn.yzw.jc2.common.transfer.model.DTransferJobRequest;
import cn.yzw.jc2.common.transfer.model.ReadRequest;
import cn.yzw.jc2.common.transfer.model.WriteRequest;
import cn.yzw.jc2.common.transfer.service.DataTransferService;
import cn.yzw.jc2.common.transfer.utils.CommonRdbmsUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 全量迁移
 *
 * @author yl
 */
@Slf4j
public class DTransferFactory implements ApplicationContextAware {

    private ApplicationContext          applicationContext;

    @Resource
    private RedisTemplate<String, Long> redisTemplate;

    @Resource
    private DataTransferService         dataTransferService;

    @Value("${application.name}")
    private String                      appName;

    private static final String         SHARDING_TABLE_SUFFIX = "_NEW";

    public void consumer(DTransferJobRequest request) {
        String cacheKey = "DTRANSFER" + appName + ":" + request.getTable() + ":" + request.getJobId();
        redisTemplate.opsForValue().set(cacheKey, request.getStartId());
        if (Objects.nonNull(request.getEndId()) && request.getEndId() != 0) {
            request.setMaxId(request.getMaxId());
        } else if (CollectionUtil.isNotEmpty(request.getIdList())) {
            request.setMaxId(Collections.max(request.getIdList()));
        } else {
            Long maxId = dataTransferService.getMaxId(request.getTable());
            request.setMaxId(maxId);
        }
        if (request.getMaxId() < request.getStartId()) {
            log.info("任务id为{}结束，因为开始id超过最大id", request.getJobId());
            return;
        }
        if (request.getThreadCount() != null && request.getThreadCount() > 1) {
            ExecutorService pool = null;
            try {
                int threadNum = Math.min(request.getThreadCount(), 10);
                pool = Executors.newFixedThreadPool(threadNum);
                List<Future> tasks = new ArrayList<>(threadNum);
                long startTime = System.currentTimeMillis();
                log.info("任务id{}分片执行开始，分片数量为{}", request.getJobId(), threadNum);
                for (int i = 0; i < threadNum; i++) {
                    tasks.add(pool.submit(() -> executorLock(request)));
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
                redisTemplate.delete(cacheKey);
            }
        } else {
            log.info("任务id{}串行执行", request.getJobId());
            executorNoLock(request);
        }

    }

    private void executorNoLock(DTransferJobRequest request) {
        while (true) {
            try {
                //批量查询
                ReadRequest readRequest = new ReadRequest();
                readRequest.setTable(request.getTable());
                readRequest.setStartId(request.getStartId());
                readRequest.setEndId(request.getEndId());
                readRequest.setIdList(request.getIdList());
                readRequest.setLimit(request.getLimit());
                readRequest.setQuerySql(request.getQuerySql());
                readRequest.setDatasourceType(request.getDatasourceType());
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

    private void executorLock(DTransferJobRequest request) {
        String cacheKey = "DTRANSFER" + appName + ":" + request.getTable() + ":" + request.getJobId();
        while (true) {
            Long executorStartId;
            Long executorEndId;

            //获取分片
            try {
                synchronized (request) {
                    executorStartId = redisTemplate.opsForValue().get(cacheKey);
                    if (Objects.isNull(executorStartId)) {
                        executorStartId = 0L;
                    }
                    executorEndId = executorStartId + request.getLimit();
                    redisTemplate.opsForValue().set(cacheKey, executorEndId);
                }
                long startTime = System.currentTimeMillis();
                log.info("任务id为{}开始分段获取数据，获取到分段区间id为{}-{}", request.getJobId(), executorStartId, executorEndId);
                if (executorStartId > request.getMaxId()) {
                    break;
                }
                ReadRequest threadParam = new ReadRequest();
                threadParam.setTable(request.getTable());
                threadParam.setStartId(executorStartId);
                threadParam.setEndId(executorEndId);
                threadParam.setIdList(request.getIdList());
                threadParam.setLimit(request.getLimit());
                threadParam.setQuerySql(request.getQuerySql());
                threadParam.setDatasourceType(request.getDatasourceType());
                List<Map<String, Object>> dataList = dataTransferService.getDataList(threadParam);
                log.info("任务id为{}分段获取数据结束，获取到分段区间id为{}-{}，一共获取到数据量为：{}，查询花费时间为{}", request.getJobId(), executorStartId,
                    dataList.size(), executorEndId, System.currentTimeMillis() - startTime);
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
                log.error("data-sync-error: 同步异常.任务id为{}，表名为{}", request.getJobId(), request.getTable(), e);
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
            if (Boolean.TRUE.equals(request.getSharding())) {
                writeRequest.setTable(request.getTable() + SHARDING_TABLE_SUFFIX);
                log.info("任务id为{}开启分库分表配置，参数传入表为{}，实际写入逻辑表为{}", request.getJobId(), request.getTable(),
                    writeRequest.getTable());
            } else {
                writeRequest.setTable(request.getTable());
            }
            List<String> columns = new ArrayList<>(dataList.get(0).keySet());
            List<String> valueHolders = new ArrayList<>(columns.size());
            for (int i = 0; i < columns.size(); i++) {
                valueHolders.add("?");
            }
            String writeTemplate = CommonRdbmsUtil.getWriteTemplate(columns, valueHolders, writeRequest.getTable());
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
            log.error("data-sync-error: 同步异常.任务id为{}，表名为{}，数据为{}", request.getJobId(), request.getTable(), dataList, e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
