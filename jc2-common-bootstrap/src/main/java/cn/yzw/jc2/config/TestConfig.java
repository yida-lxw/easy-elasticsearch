package cn.yzw.jc2.config;

import cn.yzw.jc2.client.EsQueryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {
    @Bean
    public EsQueryClient bean() {
        return new EsQueryClient();
    }
}
