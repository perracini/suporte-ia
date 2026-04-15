package com.eneng.suporte.service.impl;

import com.eneng.suporte.domain.exception.BusinessRuleException;
import com.eneng.suporte.domain.exception.ResourceNotFoundException;
import com.eneng.suporte.domain.model.AiAnalysis;
import com.eneng.suporte.domain.model.BugTicket;
import com.eneng.suporte.domain.model.FeatureTicket;
import com.eneng.suporte.domain.model.QuestionTicket;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.Ticket;
import com.eneng.suporte.domain.model.TicketStatus;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.gateway.kafka.TicketEventPublisher;
import com.eneng.suporte.gateway.llama.LlamaAnalysisResult;
import com.eneng.suporte.messaging.TicketCreatedEvent;
import com.eneng.suporte.repository.AiAnalysisRepository;
import com.eneng.suporte.repository.TicketRepository;
import com.eneng.suporte.repository.spec.TicketSpecifications;
import com.eneng.suporte.service.TicketService;
import com.eneng.suporte.service.command.CriarBugCommand;
import com.eneng.suporte.service.command.CriarFeatureCommand;
import com.eneng.suporte.service.command.CriarQuestionCommand;
import com.eneng.suporte.service.command.TicketFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    private final TicketRepository ticketRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final TicketEventPublisher eventPublisher;

    public TicketServiceImpl(TicketRepository ticketRepository,
                             AiAnalysisRepository aiAnalysisRepository,
                             TicketEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Ticket criarBug(CriarBugCommand command, User creator) {
        BugTicket ticket = new BugTicket();
        ticket.setTitle(command.title());
        ticket.setDescription(command.description());
        ticket.setStepsToReproduce(command.stepsToReproduce());
        ticket.setAffectedVersion(command.affectedVersion());
        ticket.setSeverity(command.severity());
        return persistAndPublish(ticket, creator);
    }

    @Override
    public Ticket criarFeature(CriarFeatureCommand command, User creator) {
        FeatureTicket ticket = new FeatureTicket();
        ticket.setTitle(command.title());
        ticket.setDescription(command.description());
        ticket.setBusinessValue(command.businessValue());
        ticket.setTargetVersion(command.targetVersion());
        return persistAndPublish(ticket, creator);
    }

    @Override
    public Ticket criarQuestion(CriarQuestionCommand command, User creator) {
        QuestionTicket ticket = new QuestionTicket();
        ticket.setTitle(command.title());
        ticket.setDescription(command.description());
        ticket.setCategory(command.category());
        return persistAndPublish(ticket, creator);
    }

    private Ticket persistAndPublish(Ticket ticket, User creator) {
        ticket.setCreatedBy(creator);
        ticket.setStatus(TicketStatus.OPEN);
        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publishTicketCreated(new TicketCreatedEvent(
                saved.getId(),
                saved.ticketType(),
                creator.getId(),
                Instant.now()
        ));
        log.info("Ticket criado id={} tipo={}", saved.getId(), saved.ticketType());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Ticket buscar(UUID id, User requester) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        ensureVisibility(ticket, requester);
        return ticket;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Ticket> listar(TicketFilter filter, Pageable pageable, User requester) {
        Specification<Ticket> spec = Specification.allOf(
                TicketSpecifications.byStatus(filter.status()),
                TicketSpecifications.byPriority(filter.priority()),
                TicketSpecifications.byType(filter.ticketType()),
                TicketSpecifications.visibleTo(requester)
        );
        return ticketRepository.findAll(spec, pageable);
    }

    @Override
    public Ticket assumir(UUID ticketId, User agent) {
        if (agent.getRole() == Role.CLIENT) {
            throw new AccessDeniedException("CLIENT nao pode assumir tickets");
        }
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        if (ticket.getAssignedTo() != null) {
            throw new BusinessRuleException("Ticket ja esta atribuido");
        }
        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        return ticket;
    }

    @Override
    public Ticket atualizarStatus(UUID ticketId, TicketStatus novoStatus, User user) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        ensureCanMutate(ticket, user);
        ticket.setStatus(novoStatus);
        return ticket;
    }

    @Override
    public void aplicarAnalise(UUID ticketId, LlamaAnalysisResult result) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        AiAnalysis analysis = aiAnalysisRepository.findByTicketId(ticketId)
                .orElseGet(() -> AiAnalysis.builder().ticket(ticket).build());
        analysis.setSuggestedCategory(result.suggestedCategory());
        analysis.setSuggestedPriority(result.suggestedPriority());
        analysis.setDraftReply(result.draftReply());
        analysis.setConfidence(result.confidence());
        analysis.setModelName(result.modelName());
        analysis.setPromptHash(result.promptHash());
        analysis.setFallback(result.fallback());
        aiAnalysisRepository.save(analysis);

        if (ticket.getPriority() == null) {
            ticket.setPriority(result.suggestedPriority());
        }
        ticket.setStatus(TicketStatus.IN_TRIAGE);
        log.info("Analise IA aplicada ao ticket {} prioridade={} fallback={}",
                ticketId, result.suggestedPriority(), result.fallback());
    }

    @Override
    public void aplicarAnaliseFallback(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        LlamaAnalysisResult fallback = new LlamaAnalysisResult(
                "fallback",
                ticket.prioridadeInicialSugerida(),
                "Nao foi possivel processar via IA. Um atendente ira responder em breve.",
                java.math.BigDecimal.ZERO,
                "fallback",
                "fallback:" + ticketId,
                true
        );
        aplicarAnalise(ticketId, fallback);
    }

    private void ensureVisibility(Ticket ticket, User requester) {
        if (requester.getRole() == Role.CLIENT &&
                !ticket.getCreatedBy().getId().equals(requester.getId())) {
            throw new AccessDeniedException("Voce nao pode acessar este ticket");
        }
    }

    private void ensureCanMutate(Ticket ticket, User user) {
        if (user.getRole() == Role.CLIENT &&
                !ticket.getCreatedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Voce nao pode modificar este ticket");
        }
    }
}
