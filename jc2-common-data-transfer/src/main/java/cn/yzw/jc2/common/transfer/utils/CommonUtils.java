package cn.yzw.jc2.common.transfer.utils;

import cn.yzw.infra.component.utils.AssertUtils;
import cn.yzw.infra.component.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;

@Slf4j(topic = "dtransfer")
public class CommonUtils {
    public static JdbcTemplate initJdbcTemplate(String dataSourceName) {
        String[] beanNames = SpringContextUtils.getApplicationContext().getBeanNamesForType(DataSource.class);
        AssertUtils.notEmpty(beanNames, "未获取到数据源");
        if (beanNames.length == 1) {
            log.info("获取到数据源为：{}", SpringContextUtils.getBean(beanNames[0]).toString());
            return new JdbcTemplate(SpringContextUtils.getBean(beanNames[0]));
        } else {
            AssertUtils.notBlank(dataSourceName, "存在多数据源，数据源名称不能为空");
            AssertUtils.isTrue(Arrays.asList(beanNames).contains(dataSourceName), "未匹配到数据源");
            log.info("获取到数据源为：{}", SpringContextUtils.getBean(beanNames[0]).toString());
            return new JdbcTemplate(SpringContextUtils.getBean(dataSourceName));
        }

    }
}
