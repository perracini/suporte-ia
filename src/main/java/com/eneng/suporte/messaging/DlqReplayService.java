package com.eneng.suporte.messaging;

import com.eneng.suporte.config.KafkaAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DlqReplayService {

    private static final Logger log = LoggerFactory.getLogger(DlqReplayService.class);

    private final TicketTriageDlqListener dlqListener;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaAppProperties properties;

    public DlqReplayService(TicketTriageDlqListener dlqListener,
                            KafkaTemplate<String, Object> kafkaTemplate,
                            KafkaAppProperties properties) {
        this.dlqListener = dlqListener;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    public int replay() {
        int count = 0;
        TicketCreatedEvent event;
        while ((event = dlqListener.pendingReplay().poll()) != null) {
            log.info("Reprocessando evento da DLQ ticketId={}", event.ticketId());
            kafkaTemplate.send(properties.topics().ticketCreated(), event.ticketId().toString(), event);
            count++;
        }
        return count;
    }
}
