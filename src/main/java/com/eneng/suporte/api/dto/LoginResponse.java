package com.eneng.suporte.api.dto;

import com.eneng.suporte.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Token JWT emitido apos autenticacao")
public record LoginResponse(
        @Schema(description = "Token JWT assinado (HS256)", example = "eyJhbGciOi...")
        String token,

        @Schema(description = "Tempo de expiracao em segundos a partir da emissao", example = "7200")
        long expiresInSeconds,

        @Schema(description = "Identificador do usuario autenticado", example = "c7f3d0a5-5b8a-44b1-8f6d-1e2c77a9b3e0")
        UUID userId,

        @Schema(description = "Nome do usuario autenticado", example = "cli1")
        String username,

        @Schema(description = "Papel do usuario", example = "CLIENT")
        Role role
) {
}
