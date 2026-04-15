package com.eneng.suporte.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        LlamaProperties.class,
        KafkaAppProperties.class,
        SecurityProperties.class
})
public class AppPropertiesConfig {
}
