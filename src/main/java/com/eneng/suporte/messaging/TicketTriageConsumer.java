package com.eneng.suporte.messaging;

import com.eneng.suporte.service.TicketTriageOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TicketTriageConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketTriageConsumer.class);

    private final TicketTriageOrchestrator orchestrator;

    public TicketTriageConsumer(TicketTriageOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.ticket-created}",
            containerFactory = "ticketListenerContainerFactory"
    )
    public void consumir(TicketCreatedEvent event) {
        log.info("Consumindo TicketCreatedEvent ticketId={}", event.ticketId());
        orchestrator.triar(event.ticketId());
    }
}
