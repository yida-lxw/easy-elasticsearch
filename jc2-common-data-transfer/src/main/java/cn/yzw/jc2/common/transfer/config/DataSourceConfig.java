package cn.yzw.jc2.common.transfer.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;

/**
 * @description: 数据库配置
 * @author: yangle
 * @date: 2024/10/21
 **/
@Configuration
public class DataSourceConfig {

    @Value("${spring.application.name}")
    private String appName;
    
    @Bean(name = "transferReadDataSource")
    @Qualifier("transferReadDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.transfer.read")
    public DruidDataSource transferReadDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean(name = "transferWriteDataSource")
    @Qualifier("transferWriteDataSource")
    @ConfigurationProperties(prefix = "spring.shardingsphere.datasource.ds0")
    public DruidDataSource transferWriteDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean("transferReadJdbcTemplate")
    public JdbcTemplate transferReadJdbcTemplate(@Qualifier("transferReadDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean("transferWriteJdbcTemplate")
    public JdbcTemplate transferTargetJdbcTemplate(@Qualifier("transferWriteDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "transferReadTransactionManager")
    public DataSourceTransactionManager transferSourceTransactionManager(@Qualifier("transferReadDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "transferWriteTransactionManager")
    public DataSourceTransactionManager transferTargetTransactionManager(@Qualifier("transferWriteDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    public String getAppName() {
        return appName;
    }
}
