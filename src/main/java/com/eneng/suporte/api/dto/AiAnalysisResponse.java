package com.eneng.suporte.api.dto;

import com.eneng.suporte.domain.model.Priority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Resultado da triagem automatica feita pela IA Llama")
public record AiAnalysisResponse(
        @Schema(description = "Categoria curta sugerida pela IA", example = "auth")
        String suggestedCategory,

        @Schema(description = "Prioridade sugerida pela IA", example = "HIGH")
        Priority suggestedPriority,

        @Schema(description = "Rascunho de resposta ao cliente gerado pela IA",
                example = "Identificamos o problema e nosso time ja esta investigando.")
        String draftReply,

        @Schema(description = "Confianca da IA entre 0 e 1", example = "0.87")
        BigDecimal confidence,

        @Schema(description = "Nome do modelo que produziu a analise", example = "llama3.2")
        String modelName,

        @Schema(description = "Se true, a analise foi gerada por fallback polimorfico (IA indisponivel)",
                example = "false")
        boolean fallback,

        @Schema(description = "Instante de criacao da analise", example = "2026-04-15T10:00:05Z")
        Instant createdAt
) {
}
