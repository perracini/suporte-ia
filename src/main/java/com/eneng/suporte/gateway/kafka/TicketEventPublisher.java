package com.eneng.suporte.gateway.kafka;

import com.eneng.suporte.messaging.TicketCreatedEvent;

public interface TicketEventPublisher {
    void publishTicketCreated(TicketCreatedEvent event);
}
