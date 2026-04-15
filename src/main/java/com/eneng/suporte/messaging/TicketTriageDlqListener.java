package com.eneng.suporte.messaging;

import com.eneng.suporte.service.TicketTriageOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class TicketTriageDlqListener {

    private static final Logger log = LoggerFactory.getLogger(TicketTriageDlqListener.class);

    private final TicketTriageOrchestrator orchestrator;
    private final ConcurrentLinkedQueue<TicketCreatedEvent> pendingReplay = new ConcurrentLinkedQueue<>();

    public TicketTriageDlqListener(TicketTriageOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.ticket-created-dlt}",
            groupId = "suporte-ia-dlq"
    )
    public void consumir(TicketCreatedEvent event) {
        log.warn("Evento na DLQ: ticketId={}. Aplicando analise de fallback.", event.ticketId());
        try {
            orchestrator.fallback(event.ticketId());
        } catch (Exception ex) {
            log.error("Falha ao aplicar fallback para ticket {}", event.ticketId(), ex);
        }
        pendingReplay.add(event);
    }

    public ConcurrentLinkedQueue<TicketCreatedEvent> pendingReplay() {
        return pendingReplay;
    }
}
