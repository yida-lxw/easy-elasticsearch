package cn.yzw.jc2.common.transfer.interceptor;

import java.lang.reflect.Field;
import java.sql.Connection;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

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
@ConditionalOnClass(Interceptor.class)
public class SingleReadInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(handler);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();
        // 查询操作，修改 SQL 替换为影子表
        if (sct == SqlCommandType.SELECT) {
            BoundSql boundSql = handler.getBoundSql();
            String originalSql = boundSql.getSql();
            String modifiedSql = SqlUtils.replaceTableName(originalSql, "test_user", "test");
            log.info("DualReadInterceptor execute after sql is: {}", modifiedSql);
            // 通过反射修改 BoundSql 中的 SQL 字段
            Field sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, modifiedSql);
        }else if (sct == SqlCommandType.DELETE) {
//            BoundSql boundSql = handler.getBoundSql();
//            String originalSql = boundSql.getSql();
//            String modifiedSql = SqlUtils.replaceTableName2(originalSql, "test", "test_user");
//            log.info("DualReadInterceptor execute after sql is: {}", modifiedSql);
//            // 通过反射修改 BoundSql 中的 SQL 字段
//            Field sqlField = BoundSql.class.getDeclaredField("sql");
//            sqlField.setAccessible(true);
//            sqlField.set(boundSql, modifiedSql);
        }
        // 非增、删、改操作正常执行
        return invocation.proceed();
    }

}
