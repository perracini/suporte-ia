package com.eneng.suporte.messaging;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record TicketCreatedEvent(
        UUID ticketId,
        String ticketType,
        UUID createdBy,
        Instant createdAt
) implements Serializable {
}
