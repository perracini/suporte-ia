package com.eneng.suporte.gateway.llama;

import com.eneng.suporte.config.LlamaProperties;
import com.eneng.suporte.domain.exception.LlamaUnavailableException;
import com.eneng.suporte.domain.model.Priority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;

@Component
public class OllamaLlamaAdapter implements LlamaGateway {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlamaAdapter.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final LlamaProperties properties;
    private final LlamaRateLimiter rateLimiter;

    public OllamaLlamaAdapter(LlamaProperties properties,
                              LlamaRateLimiter rateLimiter,
                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    @Cacheable(cacheNames = "llama-analysis", key = "#root.target.hash(#prompt)")
    public LlamaAnalysisResult analisar(String prompt) {
        log.debug("Chamando Llama modelo {}", properties.model());
        return rateLimiter.execute(() -> callOllama(prompt));
    }

    public String hash(String prompt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    (properties.model() + ":" + prompt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }

    private LlamaAnalysisResult callOllama(String prompt) {
        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "prompt", prompt,
                "stream", false,
                "format", "json"
        );
        try {
            String raw = restClient.post()
                    .uri("/api/generate")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseResponse(raw, prompt);
        } catch (ResourceAccessException e) {
            throw new LlamaUnavailableException("Timeout ou indisponibilidade do Ollama", e);
        } catch (RestClientException e) {
            throw new LlamaUnavailableException("Falha ao chamar Ollama: " + e.getMessage(), e);
        }
    }

    private LlamaAnalysisResult parseResponse(String raw, String prompt) {
        try {
            JsonNode envelope = objectMapper.readTree(raw);
            String inner = envelope.path("response").asText("{}");
            JsonNode json = objectMapper.readTree(inner);

            String category = json.path("suggestedCategory").asText("uncategorized");
            String priorityStr = json.path("suggestedPriority").asText("MEDIUM").toUpperCase();
            Priority priority = parsePriority(priorityStr);
            String draft = json.path("draftReply").asText("");
            BigDecimal confidence = BigDecimal.valueOf(json.path("confidence").asDouble(0.5));

            return new LlamaAnalysisResult(
                    category,
                    priority,
                    draft,
                    confidence,
                    properties.model(),
                    hash(prompt),
                    false
            );
        } catch (Exception e) {
            throw new LlamaUnavailableException("Resposta do Llama em formato invalido", e);
        }
    }

    private Priority parsePriority(String raw) {
        try {
            return Priority.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return Priority.MEDIUM;
        }
    }

    public Duration timeout() {
        return Duration.ofSeconds(properties.timeoutSeconds());
    }
}
