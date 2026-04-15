package com.eneng.suporte.gateway.kafka;

import com.eneng.suporte.config.KafkaAppProperties;
import com.eneng.suporte.messaging.TicketCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class KafkaTicketEventPublisher implements TicketEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaTicketEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaAppProperties properties;

    public KafkaTicketEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                     KafkaAppProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publishTicketCreated(TicketCreatedEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(event);
                }
            });
        } else {
            doPublish(event);
        }
    }

    private void doPublish(TicketCreatedEvent event) {
        log.info("Publicando TicketCreatedEvent ticketId={} tipo={}", event.ticketId(), event.ticketType());
        kafkaTemplate.send(properties.topics().ticketCreated(), event.ticketId().toString(), event);
    }
}
