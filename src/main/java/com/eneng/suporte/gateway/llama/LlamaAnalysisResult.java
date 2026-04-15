package com.eneng.suporte.gateway.llama;

import com.eneng.suporte.domain.model.Priority;

import java.io.Serializable;
import java.math.BigDecimal;

public record LlamaAnalysisResult(
        String suggestedCategory,
        Priority suggestedPriority,
        String draftReply,
        BigDecimal confidence,
        String modelName,
        String promptHash,
        boolean fallback
) implements Serializable {
}
