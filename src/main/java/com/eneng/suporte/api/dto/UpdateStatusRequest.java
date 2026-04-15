package com.eneng.suporte.api.dto;

import com.eneng.suporte.api.dto.validation.ValidationGroups.OnUpdate;
import com.eneng.suporte.domain.model.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload de atualizacao de status de um ticket")
public record UpdateStatusRequest(
        @Schema(description = "Novo status do ticket", example = "IN_PROGRESS",
                allowableValues = {"OPEN", "IN_TRIAGE", "IN_PROGRESS", "WAITING_CLIENT", "RESOLVED", "CLOSED"})
        @NotNull(groups = OnUpdate.class)
        TicketStatus status
) {
}
