package cn.yzw.jc2.common.transfer.interceptor;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import cn.yzw.jc2.common.transfer.config.DTransferConfig;
import cn.yzw.jc2.common.transfer.enums.ReadTypeEnum;
import cn.yzw.jc2.common.transfer.enums.WriteTypeEnum;
import cn.yzw.jc2.common.transfer.model.DTransferDoubleWriteProperties;
import cn.yzw.jc2.common.transfer.utils.PluginUtils;
import cn.yzw.jc2.common.transfer.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 单个拦截
 * @Author: lbl
 * @Date: 2024/10/17
 **/
@Component
@Slf4j
@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class,
                                                                                     Integer.class }), })
@ConditionalOnClass(DTransferConfig.class)
@Order(1)
public class SingleRWInterceptor implements Interceptor {
    @Resource
    private DTransferConfig dTransferConfig;

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
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(handler);
        MappedStatement ms = mpSh.mappedStatement();

        BoundSql boundSql = handler.getBoundSql();
        String originalSql = boundSql.getSql();
        List<String> tableNames = SqlUtils.getTableNames(originalSql);
        if (CollectionUtils.isEmpty(tableNames) || tableNames.size() > 1) {
            log.info("SingleReadInterceptor表名不符合拦截条件{}", tableNames);
            return invocation.proceed();
        }
        String oldTableName = tableNames.get(0);
        if (!writePropertiesMap.containsKey(oldTableName)
            || !Boolean.TRUE.equals(writePropertiesMap.get(oldTableName).getOpen())) {
            return invocation.proceed();
        }
        DTransferDoubleWriteProperties dTransferDoubleWriteProperties = writePropertiesMap.get(oldTableName);
        SqlCommandType sqlCommandType = ms.getSqlCommandType();

        // 查询操作，修改 SQL 替换为影子表
        if (sqlCommandType == SqlCommandType.SELECT) {
            if (WriteTypeEnum.ONLY_WRITE_NEW_TABLE.name()
                .equalsIgnoreCase(dTransferDoubleWriteProperties.getWriteType())
                || ReadTypeEnum.READ_NEW_TABLE.name()
                    .equalsIgnoreCase(dTransferDoubleWriteProperties.getReadType())) {
                String modifiedSql = SqlUtils.replaceTableName(originalSql, oldTableName,
                    dTransferDoubleWriteProperties.getNewTableName());
                log.info("SingleReadInterceptor execute after sql is: {}", modifiedSql);
                // 通过反射修改 BoundSql 中的 SQL 字段
                Field sqlField = BoundSql.class.getDeclaredField("sql");
                sqlField.setAccessible(true);
                sqlField.set(boundSql, modifiedSql);
            }

        } else if (sqlCommandType == SqlCommandType.INSERT || sqlCommandType == SqlCommandType.UPDATE
                   || sqlCommandType == SqlCommandType.DELETE) {
            if (WriteTypeEnum.ONLY_WRITE_NEW_TABLE.name()
                .equalsIgnoreCase(dTransferDoubleWriteProperties.getWriteType())) {
                String modifiedSql = SqlUtils.replaceTableName(originalSql, oldTableName,
                    dTransferDoubleWriteProperties.getNewTableName());
                log.info("SingleReadInterceptor execute after sql is: {}", modifiedSql);
                // 通过反射修改 BoundSql 中的 SQL 字段
                Field sqlField = BoundSql.class.getDeclaredField("sql");
                sqlField.setAccessible(true);
                sqlField.set(boundSql, modifiedSql);
            }
        }
        return invocation.proceed();
    }

}
