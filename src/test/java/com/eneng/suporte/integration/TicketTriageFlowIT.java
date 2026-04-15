package com.eneng.suporte.integration;

import com.eneng.suporte.domain.model.BugTicket;
import com.eneng.suporte.domain.model.Priority;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.Severity;
import com.eneng.suporte.domain.model.TicketStatus;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.gateway.llama.LlamaAnalysisResult;
import com.eneng.suporte.gateway.llama.LlamaGateway;
import com.eneng.suporte.messaging.TicketTriageDlqListener;
import com.eneng.suporte.repository.AiAnalysisRepository;
import com.eneng.suporte.repository.TicketRepository;
import com.eneng.suporte.repository.UserRepository;
import com.eneng.suporte.service.TicketService;
import com.eneng.suporte.service.command.CriarBugCommand;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@EmbeddedKafka(partitions = 1, topics = {"ticket.created", "ticket.created.DLT"})
class TicketTriageFlowIT {

    @Autowired
    TicketService ticketService;
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    AiAnalysisRepository aiAnalysisRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TicketTriageDlqListener dlqListener;

    @MockBean
    LlamaGateway llamaGateway;

    User client;

    @BeforeEach
    void setUp() {
        Mockito.reset(llamaGateway);
        dlqListener.pendingReplay().clear();
        aiAnalysisRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        client = userRepository.save(User.builder()
                .username("cli-flow")
                .email("cli-flow@e.com")
                .passwordHash("hash")
                .role(Role.CLIENT)
                .build());
    }

    @Test
    void flow_completo_publica_evento_consome_e_grava_analise() {
        when(llamaGateway.analisar(anyString())).thenReturn(new LlamaAnalysisResult(
                "auth", Priority.HIGH, "rascunho", new BigDecimal("0.9"),
                "llama3.2", "hash-abc", false
        ));

        BugTicket ticket = (BugTicket) ticketService.criarBug(
                new CriarBugCommand(
                        "Usuarios nao conseguem login",
                        "Tudo retorna 401",
                        "Abrir; logar",
                        "1.0",
                        Severity.MAJOR),
                client);

        UUID id = ticket.getId();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(aiAnalysisRepository.findByTicketId(id)).isPresent();
        });

        verify(llamaGateway, atLeastOnce()).analisar(anyString());

        BugTicket updated = (BugTicket) ticketRepository.findById(id).orElseThrow();
        assertThat(updated.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_TRIAGE);
        assertThat(aiAnalysisRepository.findByTicketId(id)).get()
                .satisfies(a -> {
                    assertThat(a.getSuggestedCategory()).isEqualTo("auth");
                    assertThat(a.isFallback()).isFalse();
                });
    }

    @Test
    void flow_com_llama_falhando_usa_fallback_via_dlq() {
        when(llamaGateway.analisar(anyString()))
                .thenThrow(new com.eneng.suporte.domain.exception.LlamaUnavailableException("down"));

        BugTicket ticket = (BugTicket) ticketService.criarBug(
                new CriarBugCommand(
                        "Falha critica",
                        "Sistema indisponivel",
                        "n/a",
                        "1.0",
                        Severity.BLOCKER),
                client);

        UUID id = ticket.getId();

        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(aiAnalysisRepository.findByTicketId(id))
                    .get()
                    .satisfies(a -> assertThat(a.isFallback()).isTrue());
        });

        verify(llamaGateway, atLeast(3)).analisar(anyString());

        BugTicket updated = (BugTicket) ticketRepository.findById(id).orElseThrow();
        assertThat(updated.getPriority()).isEqualTo(Priority.CRITICAL);
    }
}
