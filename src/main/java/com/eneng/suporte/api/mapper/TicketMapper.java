package com.eneng.suporte.api.mapper;

import com.eneng.suporte.api.dto.AiAnalysisResponse;
import com.eneng.suporte.api.dto.TicketResponse;
import com.eneng.suporte.domain.model.AiAnalysis;
import com.eneng.suporte.domain.model.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.ticketType(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCreatedBy() == null ? null : ticket.getCreatedBy().getId(),
                ticket.getAssignedTo() == null ? null : ticket.getAssignedTo().getId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                toAnalysisResponse(ticket.getAiAnalysis())
        );
    }

    public AiAnalysisResponse toAnalysisResponse(AiAnalysis analysis) {
        if (analysis == null) {
            return null;
        }
        return new AiAnalysisResponse(
                analysis.getSuggestedCategory(),
                analysis.getSuggestedPriority(),
                analysis.getDraftReply(),
                analysis.getConfidence(),
                analysis.getModelName(),
                analysis.isFallback(),
                analysis.getCreatedAt()
        );
    }
}
