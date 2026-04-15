package com.eneng.suporte.integration;

import com.eneng.suporte.domain.model.Priority;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.gateway.llama.LlamaAnalysisResult;
import com.eneng.suporte.gateway.llama.LlamaGateway;
import com.eneng.suporte.repository.AiAnalysisRepository;
import com.eneng.suporte.repository.TicketRepository;
import com.eneng.suporte.repository.UserRepository;
import com.eneng.suporte.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@EmbeddedKafka(partitions = 1, topics = {"ticket.created", "ticket.created.DLT"})
class TicketControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    AiAnalysisRepository aiAnalysisRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    JwtService jwtService;

    @MockBean
    LlamaGateway llamaGateway;

    String clientToken;
    String agentToken;

    @BeforeEach
    void setup() {
        aiAnalysisRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        User client = userRepository.save(User.builder()
                .username("clienteA")
                .email("clientea@e.com")
                .passwordHash(passwordEncoder.encode("segredo123"))
                .role(Role.CLIENT)
                .build());
        User agent = userRepository.save(User.builder()
                .username("agenteA")
                .email("agentea@e.com")
                .passwordHash(passwordEncoder.encode("segredo123"))
                .role(Role.AGENT)
                .build());
        clientToken = jwtService.issue(client).token();
        agentToken = jwtService.issue(agent).token();

        when(llamaGateway.analisar(anyString())).thenReturn(new LlamaAnalysisResult(
                "auth", Priority.HIGH, "rascunho", new BigDecimal("0.9"),
                "llama3.2", "hash", false
        ));
    }

    @Test
    void criar_bug_retorna_201_e_type_BUG() throws Exception {
        String body = """
                {
                  "title": "Falha no login",
                  "description": "Usuarios nao conseguem autenticar",
                  "stepsToReproduce": "Abrir app; Logar",
                  "affectedVersion": "1.0",
                  "severity": "MAJOR"
                }
                """;
        mockMvc.perform(post("/api/v1/tickets/bugs")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("BUG"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void criar_feature_retorna_201() throws Exception {
        String body = """
                {
                  "title": "Exportar CSV",
                  "description": "Adicionar exportacao do relatorio",
                  "businessValue": "Aumenta retencao",
                  "targetVersion": "2.0"
                }
                """;
        mockMvc.perform(post("/api/v1/tickets/features")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("FEATURE"));
    }

    @Test
    void criar_question_retorna_201() throws Exception {
        String body = """
                {
                  "title": "Como troco cartao?",
                  "description": "Preciso alterar forma de pagamento",
                  "category": "BILLING"
                }
                """;
        mockMvc.perform(post("/api/v1/tickets/questions")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("QUESTION"));
    }

    @Test
    void criar_sem_token_retorna_401() throws Exception {
        String body = """
                {"title": "t", "description": "d", "severity": "MINOR"}
                """;
        mockMvc.perform(post("/api/v1/tickets/bugs").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criar_com_validacao_falha_retorna_400() throws Exception {
        String body = """
                {"title": "x", "description": "y", "severity": "MINOR"}
                """;
        mockMvc.perform(post("/api/v1/tickets/bugs")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Entrada invalida"));
    }

    @Test
    void listar_tickets_paginado() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?page=0&size=10")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.pageable").exists());
    }
}
