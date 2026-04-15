package com.eneng.suporte.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Representacao de um comentario em um ticket")
public record CommentResponse(
        @Schema(description = "Identificador do comentario", example = "c1111111-2222-3333-4444-555555555555")
        UUID id,

        @Schema(description = "Id do ticket ao qual o comentario pertence", example = "a1b2c3d4-0000-1111-2222-333344445555")
        UUID ticketId,

        @Schema(description = "Id do autor do comentario", example = "c7f3d0a5-5b8a-44b1-8f6d-1e2c77a9b3e0")
        UUID authorId,

        @Schema(description = "Username do autor do comentario", example = "rafael")
        String authorUsername,

        @Schema(description = "Conteudo do comentario", example = "Time ja reproduziu o problema em homolog.")
        String body,

        @Schema(description = "Se true, comentario interno visivel apenas para AGENT/ADMIN", example = "false")
        boolean internal,

        @Schema(description = "Instante de criacao do comentario", example = "2026-04-15T10:05:20Z")
        Instant createdAt
) {
}
