package com.eneng.suporte.api.dto;

import com.eneng.suporte.domain.model.Priority;
import com.eneng.suporte.domain.model.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Representacao completa de um ticket")
public record TicketResponse(
        @Schema(description = "Identificador do ticket", example = "a1b2c3d4-0000-1111-2222-333344445555")
        UUID id,

        @Schema(description = "Tipo polimorfico do ticket", example = "BUG",
                allowableValues = {"BUG", "FEATURE", "QUESTION"})
        String type,

        @Schema(description = "Titulo do ticket", example = "Falha no login com Google")
        String title,

        @Schema(description = "Descricao detalhada", example = "Tela branca ao clicar em entrar com Google")
        String description,

        @Schema(description = "Status atual", example = "IN_TRIAGE")
        TicketStatus status,

        @Schema(description = "Prioridade atual (preenchida pela IA ou por AGENT/ADMIN)", example = "HIGH")
        Priority priority,

        @Schema(description = "Id do usuario que criou o ticket", example = "c7f3d0a5-5b8a-44b1-8f6d-1e2c77a9b3e0")
        UUID createdBy,

        @Schema(description = "Id do agente que assumiu o ticket (ou null)", nullable = true)
        UUID assignedTo,

        @Schema(description = "Instante de criacao (UTC)", example = "2026-04-15T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Instante da ultima atualizacao (UTC)", example = "2026-04-15T10:02:15Z")
        Instant updatedAt,

        @Schema(description = "Analise produzida pela IA (ou null se ainda nao triado)", nullable = true)
        AiAnalysisResponse aiAnalysis
) {
}
