package com.eneng.suporte.messaging;

import com.eneng.suporte.config.KafkaAppProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> ticketListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<Object, Object> kafkaTemplate,
            KafkaAppProperties properties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception ex) ->
                        new org.apache.kafka.common.TopicPartition(
                                properties.topics().ticketCreatedDlt(), record.partition())
        );

        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(properties.retry().initialIntervalMs());
        backOff.setMultiplier(properties.retry().multiplier());
        backOff.setMaxInterval(properties.retry().maxIntervalMs());
        backOff.setMaxAttempts(properties.retry().maxAttempts());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
