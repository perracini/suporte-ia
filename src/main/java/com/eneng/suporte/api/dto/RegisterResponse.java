package com.eneng.suporte.api.dto;

import com.eneng.suporte.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resposta do registro de usuario")
public record RegisterResponse(
        @Schema(description = "Identificador do usuario criado", example = "c7f3d0a5-5b8a-44b1-8f6d-1e2c77a9b3e0")
        UUID id,

        @Schema(description = "Nome de usuario escolhido", example = "cli1")
        String username,

        @Schema(description = "E-mail do usuario", example = "cli1@eneng.local")
        String email,

        @Schema(description = "Papel atribuido ao usuario", example = "CLIENT")
        Role role
) {
}
