package com.eneng.suporte.service.command;

import com.eneng.suporte.domain.model.Priority;
import com.eneng.suporte.domain.model.TicketStatus;

public record TicketFilter(
        TicketStatus status,
        Priority priority,
        String ticketType
) {
}
