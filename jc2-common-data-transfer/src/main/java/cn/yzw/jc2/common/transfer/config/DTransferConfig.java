package cn.yzw.jc2.common.transfer.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;

import cn.yzw.infra.component.utils.AssertUtils;
import cn.yzw.infra.component.utils.JsonUtils;
import cn.yzw.jc2.common.transfer.dao.impl.DataTransferDaoImpl;
import cn.yzw.jc2.common.transfer.interceptor.DoubleWriteInterceptor;
import cn.yzw.jc2.common.transfer.interceptor.SingleRWInterceptor;
import cn.yzw.jc2.common.transfer.job.DTransferJob;
import cn.yzw.jc2.common.transfer.model.DTransferDoubleWriteProperties;
import cn.yzw.jc2.common.transfer.service.DTransferService;
import cn.yzw.jc2.common.transfer.service.DataVerifyService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 配置类
 * @Author: lbl
 * @Date: 2024/10/17
 **/
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "dtransfer.double.writer")
public class DTransferConfig {

    private static final String                         DTRANSFER_DOUBLE_WRITER_MAP = "dtransfer.double.writer.list";

    /**
     * 对某表拦截双写配置
     */
    @Value("#{T(cn.yzw.jc2.common.transfer.config.DTransferConfig).decodeWriteProperties('${dtransfer.double.writer.list:null}')}")
    private List<DTransferDoubleWriteProperties>        doubleWritePropertiesList;

    private Map<String, DTransferDoubleWriteProperties> doubleWritePropertiesMap;
    /**
     * 数据迁移总开关
     */
    @Getter
    @Value("${dtransfer.double.writer.open:false}")
    private Boolean                                     open;

    @ApolloConfig
    private Config                                      applicationConfig;

    @ApolloConfig(value = "dtransfer")
    private Config                                      config;


    @Bean("dataTransferDao")
    public DataTransferDaoImpl dataTransferService() {
        return new DataTransferDaoImpl();
    }

    @Bean("dTransferService")
    public DTransferService dTransferFactory() {
        return new DTransferService();
    }

    @Bean("dTransferJob")
    public DTransferJob dTransferJob() {
        return new DTransferJob();
    }

    @Bean("dataVerifyService")
    public DataVerifyService DataVerifyService() {
        return new DataVerifyService();
    }

    @Bean("doubleWriteInterceptor")
    public DoubleWriteInterceptor doubleWriteInterceptor() {
        log.info("init mybatis DoubleWriteInterceptor");
        return new DoubleWriteInterceptor();
    }

    @Bean("singleRWInterceptor")
    public SingleRWInterceptor singleRWInterceptor() {
        log.info("init mybatis singleRWInterceptor");
        return new SingleRWInterceptor();
    }

    /**
     * @Description: 将配置转为map,表名转为小写,规范表名
     * @Author: lbl
     * @Date:  2024/10/21 14:25
     * @param:
     * @return:
     **/
    public Map<String, DTransferDoubleWriteProperties> getDoubleWritePropertiesMap() {
        if (CollectionUtils.isEmpty(doubleWritePropertiesList)) {
            return null;
        }
        if (doubleWritePropertiesMap == null) {
            synchronized (this) {
                if (doubleWritePropertiesMap == null) {
                    doubleWritePropertiesMap = new HashMap<>();
                    for (DTransferDoubleWriteProperties writeProperties : doubleWritePropertiesList) {
                        AssertUtils.notBlank(writeProperties.getOldTableName(), "老表名不能为空");
                        AssertUtils.notBlank(writeProperties.getNewTableName(), "新表名不能为空");
                        writeProperties.setOldTableName(writeProperties.getOldTableName().toLowerCase());
                        writeProperties.setNewTableName(writeProperties.getNewTableName().toLowerCase());
                        doubleWritePropertiesMap.put(writeProperties.getOldTableName(), writeProperties);
                    }
                }

            }
        }
        return doubleWritePropertiesMap;

    }

    @ApolloConfigChangeListener({ "application", "dtransfer" })
    private void configChangeListener(ConfigChangeEvent changeEvent) {

        if (changeEvent.isChanged(DTRANSFER_DOUBLE_WRITER_MAP)) {
            String jsonStr = getProperty(DTRANSFER_DOUBLE_WRITER_MAP);
            List<DTransferDoubleWriteProperties> indexRefreshConfig = JsonUtils.readAsList(jsonStr,
                DTransferDoubleWriteProperties.class);
            log.info("索引刷新配置: key: {}, result: {}", DTRANSFER_DOUBLE_WRITER_MAP, indexRefreshConfig);
            this.doubleWritePropertiesList = indexRefreshConfig;
            doubleWritePropertiesMap = null;
            getDoubleWritePropertiesMap();
        }

    }

    private String getProperty(String key) {
        String property = applicationConfig.getProperty(key, null);
        if (StringUtils.isNotBlank(property)) {
            return property;
        }
        property = config.getProperty(key, null);
        return StringUtils.isBlank(property) ? "{}" : property;
    }

    public static List<DTransferDoubleWriteProperties> decodeWriteProperties(String value) {
        return JsonUtils.readAsList(value, DTransferDoubleWriteProperties.class);
    }

}
