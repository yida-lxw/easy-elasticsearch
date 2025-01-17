package com.easy.elastic.search.config;

import com.easy.elastic.search.client.EsQueryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置
 */
@Configuration
public class EsConfig {

    @Bean("esQueryClient")
    public EsQueryClient EsQueryClient() {
        return new EsQueryClient();
    }

}

