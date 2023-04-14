package cn.yzw.jc2;

import cn.hutool.core.collection.ListUtil;
import cn.yzw.infra.component.utils.AssertUtils;
import cn.yzw.infra.component.utils.JsonUtils;
import cn.yzw.infra.component.utils.SpringContextUtils;
import cn.yzw.jc2.convert.DMigrationConvert;
import cn.yzw.jc2.enums.OperateTypeEnum;
import cn.yzw.jc2.model.CommonConstants;
import cn.yzw.jc2.model.DMigrationJobQueryDO;
import cn.yzw.jc2.model.DMigrationJobRequest;
import cn.yzw.jc2.model.Message;
import cn.yzw.jc2.thread.Jc2NamedThreadFactory;
import cn.yzw.jc2.thread.ThreadPoolMdcWrapperExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 数据迁移客户端
 *
 * @author lbl
 * @date 2022/9/13 15:08
 */
@Slf4j(topic = "DATA_MIGRATION")
public class DataExchangeDealClient<E, T> {

    @Value("${data.migrate.lock.prefix:jc1.0_}")
    private String                                     LOCK_PREFIX;
    @Autowired
    private TransactionTemplate                        transactionTemplate;

    /**
     * 待实现
     */
    @Autowired
    private DMigrationService                          dMigrationService;

    @Value("${data.migrate.need.redis.lock:true}")
    private Boolean                                    needRedisLock;

    @Value("${data.migrate.need.retry:false}")
    private Boolean                                    needRetry;

    @Autowired
    private RedissonClient                             redissonClient;

    /**
     * 待实现
     */
    @Autowired
    private List<ExchangeStrategyService>              exchangeStrategyServiceList;

    private Map<List<String>, ExchangeStrategyService> exchangeStrategyServiceMap = new HashMap<>();

    @PostConstruct
    private void init() {
        for (ExchangeStrategyService strategyService : exchangeStrategyServiceList) {
            if (CollectionUtils.isNotEmpty(strategyService.consumerTables())) {
                exchangeStrategyServiceMap.put(strategyService.consumerTables(), strategyService);
            }
        }
    }

    /**
     * 增量执行方法
     *
     * @param table   1.0表名
     * @param records 消费数据
     */
    public void kafkaConsumer(String table, List<ConsumerRecord<String, String>> records) {

        ExchangeStrategyService<E, T> service = this.getExchangeStrategyService(table);
        AssertUtils.notNull(service, "表未添加ExchangeStrategyService：" + table);
        //如果需要重试，则先记录重试任务
        List<?> bizRetryTaskModels = addRetryTaskIfNeed(table, records);
        List<Message<E>> messages = this.getMessage(records, table, service.getGenericType());
        log.info("解析kafka数据结果：{}", messages);

        //执行数据迁移逻辑
        try {
            execute(service, messages);

            //如果需要重试，当迁移成功后，更新对应任务状态
            successRetryTaskIfNeed(bizRetryTaskModels);
        } catch (Exception e) {
            if (!needRetry) {
                throw e;
            }

            log.warn("已开启重试任务，忽略异常信息 error=", e);
        }

    }

    /**
     * 任务重试
     *
     * @param table
     */
    public void RetryTaskConsumer(String table, List<ConsumerRecord<String, String>> records,
                                  List<?> bizRetryTaskModels) {

        ExchangeStrategyService<E, T> service = this.getExchangeStrategyService(table);
        AssertUtils.notNull(service, "表未添加ExchangeStrategyService：" + table);
        List<Message<E>> messages = this.getMessage(records, table, service.getGenericType());
        log.info("解析kafka数据结果：{}", messages);

        //执行数据迁移逻辑
        try {
            execute(service, messages);
            //如果需要重试，当迁移成功后，更新对应任务状态
            successRetryTaskIfNeed(bizRetryTaskModels);
        } catch (Exception e) {
            if (!needRetry) {
                throw e;
            }

            log.warn("已开启重试任务，忽略异常信息 error=", e);
        }

    }

    /**
     * @desc 全量分批执行
     * @author liangbaole
     * @date 2022/9/16
     */
    public void jobConsumer(DMigrationJobRequest request) {
        ExchangeStrategyService<E, T> service = this.getExchangeStrategyService(request.getTable());
        AssertUtils.notNull(service, "表未添加ExchangeStrategyService：" + request.getTable());
        DMigrationJobQueryDO query = DMigrationConvert.INSTANCE.convert(request);
        if (request.getThreadCount() != null && request.getThreadCount() > 1) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            int threadNum = Math.min(request.getThreadCount(), 10);
            ThreadPoolMdcWrapperExecutor pool = new ThreadPoolMdcWrapperExecutor(threadNum, threadNum, 0L,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new Jc2NamedThreadFactory("数据迁移"),
                new ThreadPoolExecutor.CallerRunsPolicy());
            List<Future> tasks = new ArrayList<>(threadNum);
            for (int i = 0; i < threadNum; i++) {
                tasks.add(pool.submit(() -> {
                    if (null != contextMap) {
                        MDC.setContextMap(contextMap);
                    }
                    executorLock(request, service, query);
                }));
            }
            tasks.forEach(e -> {
                try {
                    e.get();
                } catch (InterruptedException | ExecutionException ex) {
                    log.error("数据迁移线程执行异常", ex);
                }
            });
            //销毁线程池
            pool.shutdown();
        } else {
            executorNoLock(request, service, query);
        }

    }

    private boolean executorNoLock(DMigrationJobRequest request, ExchangeStrategyService<E, T> service,
                                   DMigrationJobQueryDO query) {
        while (true) {
            log.info("{}表查询minSysNo={}", request.getTable(), query.getMinSysNo());
            //批量查询
            List<Message<E>> messages = queryListAndBuildMessages(request, service, query);

            //本批数据处理
            if (CollectionUtils.isNotEmpty(messages)) {
                this.execute(service, messages);
            }
            if (CollectionUtils.isEmpty(messages) || messages.size() < request.getLimit()) {
                log.info("{}最后一批执行结束,maxSysNo={}", request.getTable(), query.getMinSysNo());
                break;
            }
        }
        return true;
    }

    private boolean executorLock(DMigrationJobRequest request, ExchangeStrategyService<E, T> service,
                                 DMigrationJobQueryDO query) {
        List<Message<E>> messages;
        while (true) {
            synchronized (query) {
                log.info("{}表查询minSysNo={}", request.getTable(), query.getMinSysNo());
                //批量查询
                messages = queryListAndBuildMessages(request, service, query);
            }

            //本批数据处理
            if (CollectionUtils.isNotEmpty(messages)) {
                this.execute(service, messages);
            }

            if (CollectionUtils.isEmpty(messages)) {
                log.info("{}最后一批执行结束,maxSysNo={}", request.getTable(), query.getMinSysNo());
                break;
            }

        }
        return true;
    }

    private List<Message<E>> queryListAndBuildMessages(DMigrationJobRequest request,
                                                       ExchangeStrategyService<E, T> service,
                                                       DMigrationJobQueryDO query) {
        List<Message<E>> messages;
        if (Boolean.TRUE.equals(request.getIsNeedQueryBody())) {
            List<E> list = service.queryListInterval(query.getMinSysNo(), query.getMaxSysNo(), query.getLimit(),
                query.getSysNoList());
            messages = this.getMessage(list, request.getTable());

        } else {
            List<Long> list = dMigrationService.queryList(query);
            messages = list.stream().map(item -> Message.<E> builder().sysNo(item).table(request.getTable())
                .operateType(OperateTypeEnum.UPDATE).build()).collect(Collectors.toList());
        }
        //设置下次查询起始值
        if (CollectionUtils.isNotEmpty(messages)) {
            query.setMinSysNo(messages.get(messages.size() - 1).getSysNo());
        }
        return messages;
    }

    /**
     * @desc 按操作类型过滤处理数据，删除和增删分2个事务执行，确保增改失败，不影响删除执行成功
     * @author liangbaole
     * @date 2022/9/14
     */
    private void execute(ExchangeStrategyService<E, T> service, List<Message<E>> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            log.info("messages为空");
            return;
        }
        //物理删除操作
        List<Message<E>> delMessages = messages.stream().filter(e -> OperateTypeEnum.DELETE.equals(e.getOperateType()))
            .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(delMessages)) {
            log.info("物理删除操作{}", delMessages);
            transactionTemplate.execute(status -> {
                //不影响后续增改操作
                try {
                    service.batchDelete(delMessages);
                    return true;
                } catch (Exception e) {
                    log.error("数据迁移物理删除失败:{}", e.getMessage(), e);
                }
                return false;
            });
        }

        //增改操作
        List<Message<E>> upsertMessages = messages.stream()
            .filter(e -> !OperateTypeEnum.DELETE.equals(e.getOperateType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(upsertMessages)) {
            log.info("没有增改操作{}", messages);
            return;
        }
        List<String> lockKeyList = new ArrayList<>(upsertMessages.size());
        try {
            batchUpsert(service, upsertMessages, lockKeyList);
        } finally {
            //4.释放锁
            if (CollectionUtils.isNotEmpty(lockKeyList)) {
                lockKeyList.forEach(this::unlock);
            }
        }
    }

    private void batchUpsert(ExchangeStrategyService<E, T> service, List<Message<E>> upsertMessages,
                             List<String> lockKeyList) {
        //1.加锁
        List<Message<E>> realDealMessages = new ArrayList<>(upsertMessages.size());
        if (Boolean.TRUE.equals(needRedisLock) && service.isNeedRedisLock()) {
            upsertMessages.forEach(e -> {
                String lockKey = LOCK_PREFIX + e.getTable() + "_" + e.getSysNo();
                if (tryLock(lockKey, TimeUnit.MILLISECONDS, 10)) {
                    realDealMessages.add(e);
                    lockKeyList.add(lockKey);
                }
            });
        } else {
            realDealMessages.addAll(upsertMessages);
        }
        if (CollectionUtils.isEmpty(realDealMessages)) {
            log.info("没有加锁成功的增改操作{}", upsertMessages);
            return;
        }

        //2.查询组装1.0数据
        List<T> list = service.queryDataList(realDealMessages);

        //3.写入2.0库
        if (CollectionUtils.isNotEmpty(list)) {
            transactionTemplate.execute(status -> {
                service.upsertBatch(list);
                return true;
            });
            service.upsertBatch(list);
        }
    }

    private List<Message<E>> getMessage(List<ConsumerRecord<String, String>> records, String table, Class<E> clazz) {
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }
        return records.stream().map(item -> {
            String value = item.value().substring(Math.max(0, item.value().indexOf('{')));
            Map<String, Object> jsonObject = JsonUtils.readAsMap(value);
            OperateTypeEnum operateType = OperateTypeEnum
                .getByOutsideType((String) jsonObject.get(CommonConstants.KAFKA_FIELD_OPERATE_TYPE));
            if (Objects.isNull(operateType)) {
                log.info("可能是大数据圈的数据，op：{}", jsonObject.get(CommonConstants.KAFKA_FIELD_OPERATE_TYPE));
                return null;
            }
            //提取业务主键id
            Integer sysNo = (Integer) jsonObject.get(CommonConstants.KAFKA_FIELD_SYS_NO);
            E t = JsonUtils.readAsObject(value, clazz);
            return Message.<E> builder().sysNo(Long.valueOf(sysNo)).table(table).operateType(operateType).body(t)
                .build();
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private Field getSysNoField(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        Field[] fields = clazz.getDeclaredFields();
        Field field = Arrays.stream(fields).filter(f -> f.getName().equals("SysNo")).findFirst().orElse(null);
        if (field == null) {
            return getSysNoField(clazz.getSuperclass());
        }
        return field;
    }

    private List<Message<E>> getMessage(List<E> list, String table) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.stream().map(item -> {
            //提取业务主键id
            Field sysNoField = getSysNoField(item.getClass());

            if (sysNoField == null) {
                log.error("未获取到SysNo字段{}", item);
                throw new RuntimeException("未获取到SysNo字段");
            }
            sysNoField.setAccessible(Boolean.TRUE);
            try {
                return Message.<E> builder().sysNo(Long.valueOf((Integer) sysNoField.get(item))).table(table)
                    .operateType(OperateTypeEnum.UPDATE).body(null).build();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public ExchangeStrategyService<E, T> getExchangeStrategyService(String table) {
        try {
            return SpringContextUtils.getBean(table);
        } catch (Exception e) {
            log.error("表未添加ExchangeStrategy处理器：" + table, e);
        }
        return null;
    }

    private List<?> addRetryTaskIfNeed(String table, List<ConsumerRecord<String, String>> records) {
        if (CollectionUtils.isEmpty(records)) {
            return ListUtil.empty();
        }

        if (Boolean.TRUE.equals(needRetry)) {
            try {
                List<?> list = dMigrationService.addRetryTaskIfNeed(table, records);
                log.info("新增重试任务：{}", list);
                return list;
            } catch (Exception e) {
                log.warn("新增重试任务异常，taskModels:{},error=", records, e);
            }

        }
        return ListUtil.empty();
    }

    private void successRetryTaskIfNeed(List<?> taskModels) {
        if (CollectionUtils.isEmpty(taskModels)) {
            return;
        }
        if (Boolean.TRUE.equals(needRetry)) {
            try {
                dMigrationService.batchUpdateTask(taskModels);
            } catch (Exception e) {
                log.warn("批量更新重试任务异常，taskModels:{},error=", taskModels, e);
            }
        }
    }

    /**
     * @desc 上锁后不自动释放锁
     * @author liangbaole
     * @date 2022/9/14
     */
    protected boolean tryLock(String lockKey, TimeUnit unit, int waitTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, unit);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param lockKey
     */
    protected boolean unlock(String lockKey) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
