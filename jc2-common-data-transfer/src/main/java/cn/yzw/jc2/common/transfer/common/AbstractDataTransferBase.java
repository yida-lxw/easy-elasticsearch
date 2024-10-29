package cn.yzw.jc2.common.transfer.common;

import java.util.Arrays;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import cn.yzw.infra.component.utils.AssertUtils;
import cn.yzw.infra.component.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 迁移基类
 * @Author: lbl
 * @Date: 2024/10/29
 **/
@Slf4j(topic = "dtransfer")
public abstract class AbstractDataTransferBase {
    protected JdbcTemplate jdbcTemplate;

    protected void initJdbcTemplate(String dataSourceName) {
        if (jdbcTemplate != null) {
            return;
        }
        synchronized (this) {
            if (jdbcTemplate != null) {
                return;
            }
            String[] beanNames = SpringContextUtils.getApplicationContext().getBeanNamesForType(DataSource.class);
            AssertUtils.notEmpty(beanNames, "未获取到数据源");
            if (beanNames.length == 1) {
                this.jdbcTemplate = new JdbcTemplate(SpringContextUtils.getBean(beanNames[0]));
            } else {
                AssertUtils.notBlank(dataSourceName, "数据源名称不能为空");
                AssertUtils.isTrue(Arrays.stream(beanNames).anyMatch(e -> dataSourceName.equals(e)), "未匹配到数据源");
                this.jdbcTemplate = new JdbcTemplate(SpringContextUtils.getBean(dataSourceName));
            }
        }

    }
}
