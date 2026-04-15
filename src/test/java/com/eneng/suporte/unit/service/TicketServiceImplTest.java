package com.eneng.suporte.unit.service;

import com.eneng.suporte.domain.exception.BusinessRuleException;
import com.eneng.suporte.domain.exception.ResourceNotFoundException;
import com.eneng.suporte.domain.model.AiAnalysis;
import com.eneng.suporte.domain.model.BugTicket;
import com.eneng.suporte.domain.model.Priority;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.Severity;
import com.eneng.suporte.domain.model.Ticket;
import com.eneng.suporte.domain.model.TicketStatus;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.gateway.kafka.TicketEventPublisher;
import com.eneng.suporte.gateway.llama.LlamaAnalysisResult;
import com.eneng.suporte.messaging.TicketCreatedEvent;
import com.eneng.suporte.repository.AiAnalysisRepository;
import com.eneng.suporte.repository.TicketRepository;
import com.eneng.suporte.service.command.CriarBugCommand;
import com.eneng.suporte.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    TicketRepository ticketRepository;
    @Mock
    AiAnalysisRepository aiAnalysisRepository;
    @Mock
    TicketEventPublisher eventPublisher;

    @InjectMocks
    TicketServiceImpl service;

    User client;
    User agent;

    @BeforeEach
    void setUp() {
        client = User.builder().id(UUID.randomUUID()).username("c").role(Role.CLIENT).build();
        agent = User.builder().id(UUID.randomUUID()).username("a").role(Role.AGENT).build();
    }

    @Test
    void criarBug_deve_salvar_e_publicar_evento() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            BugTicket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        Ticket ticket = service.criarBug(
                new CriarBugCommand("titulo longo", "descricao do bug bem completa", "passos", "1.0", Severity.MAJOR),
                client
        );

        assertThat(ticket).isInstanceOf(BugTicket.class);
        assertThat(ticket.getCreatedBy()).isEqualTo(client);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);

        ArgumentCaptor<TicketCreatedEvent> captor = ArgumentCaptor.forClass(TicketCreatedEvent.class);
        verify(eventPublisher).publishTicketCreated(captor.capture());
        assertThat(captor.getValue().ticketType()).isEqualTo("BUG");
    }

    @Test
    void buscar_como_client_outro_usuario_lanca_AccessDenied() {
        BugTicket ticket = new BugTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setCreatedBy(agent);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.buscar(ticket.getId(), client))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void buscar_inexistente_lanca_ResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(id, client))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assumir_como_client_deve_negar() {
        assertThatThrownBy(() -> service.assumir(UUID.randomUUID(), client))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assumir_ticket_ja_atribuido_lanca_BusinessRule() {
        BugTicket ticket = new BugTicket();
        ticket.setId(UUID.randomUUID());
        ticket.setAssignedTo(agent);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.assumir(ticket.getId(), agent))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void aplicarAnalise_preenche_prioridade_se_null() {
        BugTicket ticket = new BugTicket();
        ticket.setId(UUID.randomUUID());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(aiAnalysisRepository.findByTicketId(ticket.getId())).thenReturn(Optional.empty());
        when(aiAnalysisRepository.save(any(AiAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));

        service.aplicarAnalise(ticket.getId(), new LlamaAnalysisResult(
                "auth", Priority.HIGH, "resposta", new BigDecimal("0.9"),
                "llama3.2", "hash123", false
        ));

        assertThat(ticket.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_TRIAGE);
    }
}
