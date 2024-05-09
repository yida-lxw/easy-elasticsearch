package cn.yzw.jc2.common.util.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cn.yzw.jc2.common.util.mysql.MysqlTableFieldsUpdateExecute;
import cn.yzw.jc2.common.util.mysql.MysqlTableUpdateJob;
import lombok.Data;

@Data
@Configuration //表示这个类为配置类
public class CommonUtilConfig {

    @Bean("mysqlTableUpdateJob")
    public MysqlTableUpdateJob RetryTaskJob() {
        return new MysqlTableUpdateJob();
    }

    @Bean("mysqlTableFieldsUpdateExecute")
    public MysqlTableFieldsUpdateExecute retryTaskDomainService() {
        return new MysqlTableFieldsUpdateExecute();
    }


}
