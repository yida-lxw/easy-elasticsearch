package cn.yzw.jc2.common.transfer.interceptor;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cn.yzw.jc2.common.transfer.config.DTransferConfig;
import cn.yzw.jc2.common.transfer.model.DTransferDoubleWriteProperties;
import cn.yzw.jc2.common.transfer.enums.WriteTypeEnum;
import cn.yzw.jc2.common.transfer.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 双写拦截
 * @Author: lbl 
 * @Date: 2024/10/17
 **/
@Slf4j
@Component
@Intercepts({ @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }), })
@ConditionalOnClass(DTransferConfig.class)
public class DoubleWriteInterceptor implements Interceptor {

    @Resource
    private DTransferConfig            dTransferConfig;
    @Resource
    private PlatformTransactionManager transactionManager;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!Boolean.TRUE.equals(dTransferConfig.getOpen())) {
            return invocation.proceed();
        }
        Map<String, DTransferDoubleWriteProperties> writePropertiesMap = dTransferConfig
                .getDoubleWritePropertiesMap();
        if (MapUtils.isEmpty(writePropertiesMap)) {
            return invocation.proceed();
        }

        // 获取目标方法和参数
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        // 执行 B 表的操作
        Executor executor = (Executor) invocation.getTarget();
        // 获取 SQL 类型
        SqlCommandType sqlCommandType = ms.getSqlCommandType();

        // 判断是否是增、删、改操作
        if (sqlCommandType == SqlCommandType.INSERT || sqlCommandType == SqlCommandType.UPDATE
            || sqlCommandType == SqlCommandType.DELETE) {

            Configuration configuration = ms.getConfiguration();
            String originalSql = ms.getBoundSql(parameter).getSql();
            List<String> tableNames = SqlUtils.getTableNames(originalSql);
            if (CollectionUtils.isEmpty(tableNames) || tableNames.size() > 1) {
                log.info("DoubleWriteInterceptor表名不符合拦截条件{}", tableNames);
                return invocation.proceed();
            }
            String oldTableName = tableNames.get(0);
            if (!writePropertiesMap.containsKey(oldTableName)
                || !Boolean.TRUE.equals(writePropertiesMap.get(oldTableName).getOpen())) {
                return invocation.proceed();
            }
            log.info("DoubleWriteInterceptor拦截表{}", oldTableName);
            DTransferDoubleWriteProperties dTransferDoubleWriteProperties = writePropertiesMap.get(oldTableName);

            if (!WriteTypeEnum.WRITE_ALL_TABLE.name().equalsIgnoreCase(dTransferDoubleWriteProperties.getWriteType())) {
                return invocation.proceed();
            }
            //双写开始
            log.info("DoubleWriteInterceptor双写开始{}", oldTableName);

            // 替换表名：假设 A 表名为 'table_a'，B 表名为 'table_b'
            String modifiedSql = SqlUtils.replaceTableName(originalSql, oldTableName,
                dTransferDoubleWriteProperties.getNewTableName());
            log.info("DoubleWriteInterceptor生成新表sql {}", modifiedSql);

            // 创建新的 BoundSql
            BoundSql newBoundSql = new BoundSql(configuration, modifiedSql,
                ms.getBoundSql(parameter).getParameterMappings(), parameter);
            // 复制额外的参数
            for (ParameterMapping mapping : ms.getBoundSql(parameter).getParameterMappings()) {
                String prop = mapping.getProperty();
                if (ms.getBoundSql(parameter).hasAdditionalParameter(prop)) {
                    newBoundSql.setAdditionalParameter(prop, ms.getBoundSql(parameter).getAdditionalParameter(prop));
                }
            }

            // 创建新的 MappedStatement
            MappedStatement newMs = copyFromMappedStatement(ms, new BoundSqlSqlSource(newBoundSql));
            // 检查是否有活动的事务
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                // 如果有事务，直接执行 A 和 B 表操作
                // 执行 A 表的操作
                Object result = invocation.proceed();
                // 执行 B 表的操作
                executor.update(newMs, parameter);
                return result;
            } else {
                // 如果没有事务，手动开启事务，执行 A 和 B 表操作
                TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
                try {
                    // 执行 A 表的操作
                    Object result = invocation.proceed();
                    // 执行 B 表的操作
                    executor.update(newMs, parameter);
                    // 提交事务
                    transactionManager.commit(status);
                    return result;
                } catch (Exception e) {
                    // 回滚事务
                    transactionManager.rollback(status);
                    throw e; // 抛出异常，避免数据不一致
                }
            }
        }

        // 非增、删、改操作正常执行
        return invocation.proceed();
    }

    /**
     * 复制 MappedStatement 并替换为新的 SqlSource
     */
    private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId() + "_dual",
            newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(String.join(",", ms.getKeyProperties()));
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }

    /**
     * 简单的 SqlSource 实现，用于包装修改后的 SQL
     */
    public static class BoundSqlSqlSource implements SqlSource {
        private BoundSql boundSql;

        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }

    /**
     * 生成MyBatis拦截器代理对象
     */
    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            // 调用插件
            return Plugin.wrap(target, this);
        }
        return target;
    }

}
