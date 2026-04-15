package com.eneng.suporte.repository.spec;

import com.eneng.suporte.domain.model.Priority;
import com.eneng.suporte.domain.model.Ticket;
import com.eneng.suporte.domain.model.TicketStatus;
import com.eneng.suporte.domain.model.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> byStatus(TicketStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> byPriority(Priority priority) {
        if (priority == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> byType(String ticketType) {
        if (ticketType == null || ticketType.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.type().as(String.class), ticketType);
    }

    public static Specification<Ticket> byCreatedBy(UUID userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), userId);
    }

    public static Specification<Ticket> visibleTo(User user) {
        if (user == null) {
            return null;
        }
        return switch (user.getRole()) {
            case CLIENT -> byCreatedBy(user.getId());
            case AGENT, ADMIN -> null;
        };
    }
}
