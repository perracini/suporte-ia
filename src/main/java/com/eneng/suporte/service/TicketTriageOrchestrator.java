package com.eneng.suporte.service;

import com.eneng.suporte.domain.exception.ResourceNotFoundException;
import com.eneng.suporte.domain.model.Ticket;
import com.eneng.suporte.gateway.llama.LlamaAnalysisResult;
import com.eneng.suporte.gateway.llama.LlamaGateway;
import com.eneng.suporte.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class TicketTriageOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TicketTriageOrchestrator.class);

    private final TicketRepository ticketRepository;
    private final LlamaGateway llamaGateway;
    private final TicketService ticketService;

    public TicketTriageOrchestrator(TicketRepository ticketRepository,
                                    LlamaGateway llamaGateway,
                                    TicketService ticketService) {
        this.ticketRepository = ticketRepository;
        this.llamaGateway = llamaGateway;
        this.ticketService = ticketService;
    }

    @Transactional(readOnly = true)
    public String buildPrompt(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        return ticket.promptParaTriagem();
    }

    public void triar(UUID ticketId) {
        String prompt = buildPrompt(ticketId);
        LlamaAnalysisResult result = llamaGateway.analisar(prompt);
        ticketService.aplicarAnalise(ticketId, result);
        log.info("Triagem concluida para ticket {}", ticketId);
    }

    public void fallback(UUID ticketId) {
        ticketService.aplicarAnaliseFallback(ticketId);
    }
}
