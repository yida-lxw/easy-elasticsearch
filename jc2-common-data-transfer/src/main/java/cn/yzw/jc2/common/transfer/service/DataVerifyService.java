package cn.yzw.jc2.common.transfer.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StopWatch;

import cn.yzw.jc2.common.transfer.enums.VerifyModeEnum;
import cn.yzw.jc2.common.transfer.enums.VerifyTypeEnum;
import cn.yzw.jc2.common.transfer.model.DTransferVerifyJobRequest;
import cn.yzw.jc2.common.transfer.model.DTransferVerifyJobResponse;
import cn.yzw.jc2.common.transfer.utils.SqlUtils;
import cn.yzw.jc2.common.util.thread.Jc2NamedThreadFactory;
import cn.yzw.jc2.common.util.thread.ThreadPoolMdcWrapperExecutor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 数据核对
 * @Author: lbl
 * @Date: 2024/10/24
 **/
@Slf4j(topic = "dtransfer")
public class DataVerifyService {

    /**
     * @Description: 数据核对方法
     * @Author: lbl 
     * @Date:  2024/10/29 15:56
     * @param:
     * @return:
     **/
    public DTransferVerifyJobResponse verifyData(DTransferVerifyJobRequest request, JdbcTemplate jdbcTemplate) {
        DTransferVerifyJobResponse response = new DTransferVerifyJobResponse(new AtomicLong(0), new AtomicLong(0), 0L,
            0L);
        ThreadPoolMdcWrapperExecutor executor = creatThreadPool(request);
        //是否分配线程
        boolean splitPoolSize = !VerifyTypeEnum.COMPARE_NEW_TABLE_BY_OLD_TABLE.name()
            .equalsIgnoreCase(request.getVerifyType())
                                || !VerifyTypeEnum.COMPARE_OLD_TABLE_BY_NEW_TABLE.name()
                                    .equalsIgnoreCase(request.getVerifyType());

        //基于老表对新表的字段一致性进行比较，不一致，更新新表数据
        if (VerifyTypeEnum.COMPARE_ALL.name().equals(request.getVerifyType())
            || VerifyTypeEnum.COMPARE_NEW_TABLE_BY_OLD_TABLE.name().equalsIgnoreCase(request.getVerifyType())) {
            int threadNum;
            if (splitPoolSize) {
                int remainThreadNum = executor.getCorePoolSize() - 2;
                threadNum = remainThreadNum % 2 == 0 ? remainThreadNum / 2 : remainThreadNum / 2 + 1;
            } else {
                threadNum = executor.getCorePoolSize() - 1;
            }
            executor.execute(() -> verifyDataForUpdateNewTableByOldTable(request, response, executor, threadNum,jdbcTemplate));
        }

        //基于新表查询数据在老表是否存在，不存在则删除新表数据
        if (VerifyTypeEnum.COMPARE_ALL.name().equals(request.getVerifyType())
            || VerifyTypeEnum.COMPARE_OLD_TABLE_BY_NEW_TABLE.name().equalsIgnoreCase(request.getVerifyType())) {
            int threadNum;
            if (splitPoolSize) {
                int remainThreadNum = executor.getCorePoolSize() - 2;
                threadNum = remainThreadNum / 2;
            } else {
                threadNum = executor.getCorePoolSize() - 1;
            }
            executor.execute(() -> verifyDataForDelNewTableNotExistInOldTable(request, response, executor, threadNum,jdbcTemplate));
        }
        return response;
    }

    private static ThreadPoolMdcWrapperExecutor creatThreadPool(DTransferVerifyJobRequest request) {
        int threadNum = request.getThreadNum() == null
            ? (VerifyTypeEnum.COMPARE_NEW_TABLE_BY_OLD_TABLE.name().equalsIgnoreCase(request.getVerifyType())
               || VerifyTypeEnum.COMPARE_OLD_TABLE_BY_NEW_TABLE.name().equalsIgnoreCase(request.getVerifyType()) ? 1
                   : 2)
            : request.getThreadNum();
        return new ThreadPoolMdcWrapperExecutor(threadNum, threadNum, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200), new Jc2NamedThreadFactory("verify-data"),
            new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * @Description: 基于新表查询数据在老表是否存在，不存在则删除新表数据
     * @Author: lbl
     * @Date: 2024/10/24 15:35
     * @param:
     * @return:
     **/
    /**
     * @Description: 基于新表查询数据在老表是否存在，不存在则删除新表数据
     * @Author: lbl
     * @Date: 2024/10/24 15:35
     * @param:
     * @return:
     **/
    private void verifyDataForDelNewTableNotExistInOldTable(DTransferVerifyJobRequest request,
                                                            DTransferVerifyJobResponse response,
                                                            ThreadPoolMdcWrapperExecutor executor, int threadNum, JdbcTemplate jdbcTemplate) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        DTransferVerifyJobRequest.NewTable newTable = request.getNewTable();
        for (int i = newTable.getNewTableStartShardNum() == null ? 0 : newTable.getNewTableStartShardNum(); i < newTable
            .getNewTableShardNum(); i++) {
            String realTableName = newTable.getNewTableRealNamePrefix() + i;
            StopWatch stopWatchSub = new StopWatch();
            stopWatchSub.start();
            log.info("基于新表{}核对开始", realTableName);
            AtomicLong id = new AtomicLong(Long.MIN_VALUE);
            if (threadNum > 1) {
                List<Future> tasks = new ArrayList<>(threadNum);
                for (int j = 0; j < threadNum; j++) {
                    tasks.add(executor.submit(() -> queryAndDeal(request, response, realTableName, id,jdbcTemplate)));
                }
                tasks.forEach(e -> {
                    try {
                        e.get();
                    } catch (InterruptedException | ExecutionException ex) {
                        log.error("数据迁移线程执行异常", ex);
                    }
                });
            } else {
                queryAndDeal(request, response, realTableName, id,jdbcTemplate);
            }
            stopWatchSub.stop();
            log.info("基于新表{}核对结束,耗时{}毫秒", realTableName, stopWatchSub.getTotalTimeMillis());
        }
        stopWatch.stop();
        log.info("基于新表数据核对结束，核对数据{}条，删除新表数据{}条,总计耗时{}毫秒", response.getVerifyNewTableCount(),
            response.getVerifyDelCount().get(), stopWatch.getTotalTimeMillis());
    }

    private void queryAndDeal(DTransferVerifyJobRequest request, DTransferVerifyJobResponse response,
                              String realTableName, AtomicLong startId, JdbcTemplate jdbcTemplate) {
        while (true) {
            // 1. 从新表中分批读取数据
            List<Map<String, Object>> oldRows = getRows(request, realTableName, startId, response,jdbcTemplate);
            if (CollectionUtils.isEmpty(oldRows)) {
                break;
            }

            // 2. 新表数据在老表是否存在
            existOldTableRows(oldRows, request, response, realTableName,jdbcTemplate);
        }
    }

    private synchronized List<Map<String, Object>> getRows(DTransferVerifyJobRequest request, String realTableName,
                                                           AtomicLong startId, DTransferVerifyJobResponse response, JdbcTemplate jdbcTemplate) {
        List<Map<String, Object>> newRows = jdbcTemplate.queryForList(
            String.format("SELECT %S,id FROM %s where id>? order by id asc LIMIT ?", request.getPrimaryKeyName(), realTableName),
            startId.get(), request.getLimit());
        // 如果没有数据，退出循环
        if (CollectionUtils.isEmpty(newRows)) {
            return null;
        }
        response.setVerifyNewTableCount(response.getVerifyNewTableCount() + newRows.size());
        Object id = newRows.get(newRows.size() - 1).get("id");
        if (id instanceof BigInteger) {
            BigInteger valId = (BigInteger) id;
            startId.set(valId.longValue());
        } else if (id instanceof Long) {
            Long valId = (Long) id;
            startId.set(valId);
        } else if (id instanceof Integer) {
            Integer valId = (Integer) id;
            startId.set(Long.valueOf(valId));
        } else {
            throw new RuntimeException("不能解析的id类型");
        }

        return newRows;
    }
    //    private void verifyDataForDelNewTableNotExistInOldTable(DTransferVerifyJobRequest request,
    //                                                            DTransferVerifyJobResponse response,
    //                                                            ThreadPoolMdcWrapperExecutor executor, int threadNum) {
    //        StopWatch stopWatch = new StopWatch();
    //        stopWatch.start();
    //        DTransferVerifyJobRequest.NewTable newTable = request.getNewTable();
    //        int tableCount = newTable.getNewTableShardNum()
    //                         - (newTable.getNewTableStartShardNum() == null ? 0 : newTable.getNewTableStartShardNum());
    //        Integer start = newTable.getNewTableStartShardNum();
    //        //按线程数分批跑
    //        if (threadNum > 1) {
    //            int tablesVeryThread = tableCount / threadNum;
    //            int remainTables = tableCount % threadNum;
    //            List<Future> tasks = new ArrayList<>(threadNum);
    //            for (int i = 0; i < threadNum; i++) {
    //                int tables = tablesVeryThread;
    //                if (i < remainTables) {
    //                    tables++;
    //                }
    //                int startTableIdx = start;
    //                int endTableIdx = start + tables;
    //                tasks.add(executor.submit(
    //                    () -> verifyDataForDelNewTableNotExistInOldTable(request, response, startTableIdx, endTableIdx)));
    //                start = endTableIdx + 1;
    //            }
    //            tasks.forEach(e -> {
    //                try {
    //                    e.get();
    //                } catch (InterruptedException | ExecutionException ex) {
    //                    log.error("数据迁移线程执行异常", ex);
    //                }
    //            });
    //        } else {
    //            verifyDataForDelNewTableNotExistInOldTable(request, response, start, newTable.getNewTableShardNum());
    //        }
    //        stopWatch.stop();
    //        log.info("新表数据核对结束，删除数据{}条,耗时{}毫秒", response.getVerifyUpdateCount().get(), stopWatch.getTotalTimeMillis());
    //    }

    //    private void verifyDataForDelNewTableNotExistInOldTable(DTransferVerifyJobRequest request,
    //                                                            DTransferVerifyJobResponse response, int startTableIdx,
    //                                                            int endTableIdx) {
    //        DTransferVerifyJobRequest.NewTable newTable = request.getNewTable();
    //
    //        for (int i = startTableIdx; i < endTableIdx; i++) {
    //            String realTableName = newTable.getNewTableRealNamePrefix() + i;
    //            long offset = 0;
    //            while (true) {
    //                // 1. 从新表中分批读取数据
    //                List<Map<String, Object>> oldRows = jdbcTemplate.queryForList(
    //                    String.format("SELECT %s FROM %s LIMIT ?, ?", request.getPrimaryKey(), realTableName), offset,
    //                    request.getLimit());
    //                // 如果没有数据，退出循环
    //                if (CollectionUtils.isEmpty(oldRows)) {
    //                    break;
    //                }
    //                // 2. 新表数据在老表是否存在
    //                existOldTableRows(oldRows, request, response, realTableName);
    //
    //                // 3. 更新偏移量
    //                offset += request.getLimit();
    //            }
    //
    //        }
    //    }

    /**
     * @Description: 基于老表对新表的字段一致性进行比较，不一致，更新新表数据
     * @Author: lbl 
     * @Date:  2024/10/29 15:57
     * @param:
     * @return:
     **/
    private void existOldTableRows(List<Map<String, Object>> newRows, DTransferVerifyJobRequest request,
                                   DTransferVerifyJobResponse response, String realTableName, JdbcTemplate jdbcTemplate) {
        // 创建查询条件的 Map，方便批量查询
        Set<Object> primaryValues = newRows.stream().map(row -> row.get(request.getPrimaryKeyName()))
            .collect(Collectors.toSet());

        // 生成 SQL 查询语句
        String sql = String.format("SELECT %s FROM %s WHERE %s IN (%s)", request.getPrimaryKeyName(),
            request.getOldTable(), request.getPrimaryKeyName(),
            String.join(",", Collections.nCopies(primaryValues.size(), "?")));

        // 批量查询新表数据
        List<Map<String, Object>> oldRows = jdbcTemplate.queryForList(sql, primaryValues.toArray());

        // 将新表数据按照主键和分片键生成 Map 以便快速查找
        Set<Object> oldPrimaryValueSet = oldRows.stream().map(row -> row.get(request.getPrimaryKeyName()))
            .collect(Collectors.toSet());

        // 对比老表与新表的数据
        for (Map<String, Object> newRow : newRows) {
            Object primaryKeyValue = newRow.get(request.getPrimaryKeyName());
            if (!oldPrimaryValueSet.contains(primaryKeyValue)) {
                log.info("老表中无主键值为{}的记录", primaryKeyValue);
                if (VerifyModeEnum.ONLY_READ.name().equalsIgnoreCase(request.getVerifyMode())) {
                    continue;
                }
                // 执行删除操作
                String updateSql = String.format("DELETE FROM %s WHERE %s = ?", realTableName,
                    request.getPrimaryKeyName());
                jdbcTemplate.update(updateSql, primaryKeyValue);
                response.getVerifyDelCount().getAndIncrement();
                log.info(String.format("已删除新表%s中主键为 %s 的记录", realTableName, primaryKeyValue));
            }
        }
    }

    /**
     * @Description: 基于老表对新表的字段一致性进行比较，不一致，更新新表数据
     * @Author: lbl
     * @Date: 2024/10/24 15:35
     * @param:
     * @return:
     **/
    private void verifyDataForUpdateNewTableByOldTable(DTransferVerifyJobRequest request,
                                                       DTransferVerifyJobResponse response,
                                                       ThreadPoolMdcWrapperExecutor executor, int threadNum, JdbcTemplate jdbcTemplate) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (threadNum > 1) {
            List<Future> tasks = new ArrayList<>(threadNum);
            for (int i = 0; i < threadNum; i++) {
                tasks.add(executor.submit(() -> verifyDataForUpdateNewTableByOldTable(request, response,jdbcTemplate)));
            }
            tasks.forEach(e -> {
                try {
                    e.get();
                } catch (InterruptedException | ExecutionException ex) {
                    log.error("数据迁移线程执行异常", ex);
                }
            });
        } else {
            verifyDataForUpdateNewTableByOldTable(request, response,jdbcTemplate);
        }
        stopWatch.stop();
        log.info("基于老表核对新表字段结束，核对数据{}条，更新新表数据{}条,总计耗时{}毫秒", response.getVerifyOldTableCount(),
            response.getVerifyUpdateCount().get(), stopWatch.getTotalTimeMillis());
    }

    private void verifyDataForUpdateNewTableByOldTable(DTransferVerifyJobRequest request,
                                                       DTransferVerifyJobResponse response, JdbcTemplate jdbcTemplate) {
        while (true) {
            // 1. 从老表中分批读取数据
            List<Map<String, Object>> oldRows = queryRows(request, response,jdbcTemplate);
            // 如果没有数据，退出循环
            if (CollectionUtils.isEmpty(oldRows)) {
                break;
            }
            // 2. 比较数据是否一致,不一致做更新
            compareRows(oldRows, request, response,jdbcTemplate);
        }
    }

    private synchronized List<Map<String, Object>> queryRows(DTransferVerifyJobRequest request,
                                                             DTransferVerifyJobResponse response, JdbcTemplate jdbcTemplate) {
        log.info("{}表查询minId={}", request.getOldTable(), request.getOldTableStartId());
        String sql = SqlUtils.buildSql(request);
        List<Map<String, Object>> oldRows = jdbcTemplate.queryForList(sql, request.getLimit());
        if (CollectionUtils.isEmpty(oldRows)) {
            return null;
        }
        response.setVerifyOldTableCount(response.getVerifyOldTableCount() + oldRows.size());

        Map<String, Object> map = oldRows.get(oldRows.size() - 1);
        Object id = map.get("id");
        if (id instanceof BigInteger) {
            BigInteger valId = (BigInteger) id;
            request.setOldTableStartId(valId.longValue());
        } else if (id instanceof Long) {
            Long valId = (Long) id;
            request.setOldTableStartId(valId);
        } else if (id instanceof Integer) {
            Integer valId = (Integer) id;
            request.setOldTableStartId(Long.valueOf(valId));
        } else {
            throw new RuntimeException("不能解析的id类型");
        }

        return oldRows;
    }

    /**
     * @Description: 比较老表和新表的两组数据
     * @Author: lbl 
     * @Date:  2024/10/29 15:57
     * @param:
     * @return:
     **/
    private void compareRows(List<Map<String, Object>> oldRows, DTransferVerifyJobRequest request,
                             DTransferVerifyJobResponse response, JdbcTemplate jdbcTemplate) {
        // 创建查询条件的 Map，方便批量查询
        Set<Object> primaryKeys = oldRows.stream().map(row -> row.get(request.getPrimaryKeyName()))
            .collect(Collectors.toSet());
        Set<Object> shardingKeys = oldRows.stream().map(row -> row.get(request.getShardingKeyName()))
            .collect(Collectors.toSet());

        // 生成 SQL 查询语句
        String sql = String.format("SELECT * FROM %s WHERE %s IN (%s) AND %s IN (%s)",
            request.getNewTable().getNewTableLogicName(), request.getPrimaryKeyName(),
            String.join(",", Collections.nCopies(primaryKeys.size(), "?")), request.getShardingKeyName(),
            String.join(",", Collections.nCopies(shardingKeys.size(), "?")));

        // 批量查询新表数据
        // 合并 primaryKeys 和 shardingKeys 为一个参数数组
        List<Object> parameters = new ArrayList<>();
        parameters.addAll(primaryKeys);
        parameters.addAll(shardingKeys);
        // 使用合并后的参数数组执行查询
        List<Map<String, Object>> newRows = jdbcTemplate.queryForList(sql, parameters.toArray());

        // 将新表数据按照主键和分片键生成 Map 以便快速查找
        Map<Object, Map<String, Object>> newRowsMap = newRows.stream()
            .collect(Collectors.toMap(row -> row.get(request.getPrimaryKeyName()), Function.identity(), (a, b) -> a));

        // 对比老表与新表的数据
        for (Map<String, Object> oldRow : oldRows) {
            Object primaryKeyValue = oldRow.get(request.getPrimaryKeyName());
            Object shardingKeyValue = oldRow.get(request.getPrimaryKeyName());
            Map<String, Object> newRow = newRowsMap.get(primaryKeyValue);
            if (newRow == null) {
                log.warn("新表中缺少主键值为{}和分片键值为{}的记录", primaryKeyValue, shardingKeyValue);
                continue;
            }

            // 对比每个字段，并更新新表
            List<String> updateFields = new ArrayList<>();
            List<Object> updateValues = new ArrayList<>();
            Collection<String> columns = CollectionUtils.isNotEmpty(request.getColumns()) ? request.getColumns()
                : oldRow.keySet();
            for (String column : columns) {
                Object oldValue = oldRow.get(column);
                Object newValue = newRow.get(column);
                if (CollectionUtils.isNotEmpty(request.getIgnoreColumns())
                    && request.getIgnoreColumns().contains(column)) {
                    continue;
                }
                if (!Objects.equals(oldValue, newValue)) {
                    log.warn(String.format("主键为 %s 和分片键为 %s 的记录在字段 %s 上不一致, 老表值: %s, 新表值: %s", primaryKeyValue,
                        shardingKeyValue, column, oldValue, newValue));

                    // 准备更新新表的字段
                    updateFields.add(column + " = ?");
                    updateValues.add(oldValue); // 使用老表的值更新新表
                }
            }

            if (!updateFields.isEmpty()) {
                if (VerifyModeEnum.ONLY_READ.name().equalsIgnoreCase(request.getVerifyMode())) {
                    continue;
                }
                // 执行更新操作
                String updateSql = String.format("UPDATE %s SET %s WHERE %s = ? AND %s = ?",
                    request.getNewTable().getNewTableLogicName(), String.join(", ", updateFields),
                    request.getPrimaryKeyName(), request.getShardingKeyName());
                updateValues.add(primaryKeyValue);
                updateValues.add(shardingKeyValue);
                jdbcTemplate.update(updateSql, updateValues.toArray());
                response.getVerifyUpdateCount().getAndIncrement();
                log.info(String.format("已更新新表中主键为 %s 和分片键为 %s 的记录", primaryKeyValue, shardingKeyValue));
            }
        }
    }

}
