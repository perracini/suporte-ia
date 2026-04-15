package com.eneng.suporte.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic ticketCreatedTopic(KafkaAppProperties properties) {
        return TopicBuilder.name(properties.topics().ticketCreated())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ticketCreatedDltTopic(KafkaAppProperties properties) {
        return TopicBuilder.name(properties.topics().ticketCreatedDlt())
                .partitions(1)
                .replicas(1)
                .build();
    }
}
