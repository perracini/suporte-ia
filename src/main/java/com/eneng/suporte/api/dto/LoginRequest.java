package com.eneng.suporte.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de login")
public record LoginRequest(
        @Schema(description = "Nome de usuario", example = "cli1")
        @NotBlank String username,

        @Schema(description = "Senha em texto puro (sera verificada contra o hash BCrypt)", example = "segredo123")
        @NotBlank String password
) {
}
