package com.eneng.suporte.integration;

import com.eneng.suporte.gateway.llama.LlamaGateway;
import com.eneng.suporte.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@EmbeddedKafka(partitions = 1, topics = {"ticket.created", "ticket.created.DLT"})
class AuthControllerIT {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;

    @MockBean
    LlamaGateway llamaGateway;

    @BeforeEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    void register_e_login_retornam_token() throws Exception {
        String register = """
                {"username": "cli1", "email": "cli1@e.com", "password": "segredo123", "role": "CLIENT"}
                """;
        mockMvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(register))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("cli1"))
                .andExpect(jsonPath("$.email").value("cli1@e.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"));

        String login = """
                {"username": "cli1", "password": "segredo123"}
                """;
        mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void register_invalido_retorna_400_com_problem_detail() throws Exception {
        String invalido = """
                {"username": "x", "email": "naoemail", "password": "123", "role": "CLIENT"}
                """;
        mockMvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Entrada invalida"));
    }

    @Test
    void login_credenciais_invalidas_retorna_401() throws Exception {
        String login = """
                {"username": "inexistente", "password": "x"}
                """;
        mockMvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON).content(login))
                .andExpect(status().isNotFound());
    }
}
