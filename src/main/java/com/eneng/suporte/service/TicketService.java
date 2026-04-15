package com.eneng.suporte.service;

import com.eneng.suporte.domain.model.Ticket;
import com.eneng.suporte.domain.model.TicketStatus;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.gateway.llama.LlamaAnalysisResult;
import com.eneng.suporte.service.command.CriarBugCommand;
import com.eneng.suporte.service.command.CriarFeatureCommand;
import com.eneng.suporte.service.command.CriarQuestionCommand;
import com.eneng.suporte.service.command.TicketFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TicketService {

    Ticket criarBug(CriarBugCommand command, User creator);

    Ticket criarFeature(CriarFeatureCommand command, User creator);

    Ticket criarQuestion(CriarQuestionCommand command, User creator);

    Ticket buscar(UUID id, User requester);

    Page<Ticket> listar(TicketFilter filter, Pageable pageable, User requester);

    Ticket assumir(UUID ticketId, User agent);

    Ticket atualizarStatus(UUID ticketId, TicketStatus novoStatus, User user);

    void aplicarAnalise(UUID ticketId, LlamaAnalysisResult result);

    void aplicarAnaliseFallback(UUID ticketId);
}
