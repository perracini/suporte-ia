package com.eneng.suporte.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Evento aguardando replay na DLQ in-memory")
public record DlqEntryResponse(
        @Schema(description = "Id do ticket cujo evento falhou na triagem",
                example = "25e5a679-ef0e-45b2-a7c6-a4ce727287fe")
        UUID ticketId,

        @Schema(description = "Tipo do ticket conforme o discriminador JPA", example = "BUG")
        String ticketType,

        @Schema(description = "Titulo do ticket (quando ainda existe no banco)",
                example = "Sistema lento apos deploy")
        String title,

        @Schema(description = "Id do autor do ticket")
        UUID createdBy,

        @Schema(description = "Timestamp de criacao do evento original",
                example = "2026-04-15T14:23:37Z")
        Instant enqueuedAt
) {
}
