package com.eneng.suporte.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do reprocessamento da DLQ")
public record DlqReplayResponse(
        @Schema(description = "Quantidade de eventos reenviados ao topico principal", example = "3")
        int replayed
) {
}
