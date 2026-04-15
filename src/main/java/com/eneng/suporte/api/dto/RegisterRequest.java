package com.eneng.suporte.api.dto;

import com.eneng.suporte.api.dto.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de registro de usuario. O papel sempre sera CLIENT; "
        + "criacao de AGENT/ADMIN e feita via seeder ou endpoint administrativo.")
public record RegisterRequest(
        @Schema(description = "Nome de usuario unico", example = "cli1", minLength = 3, maxLength = 60)
        @NotBlank(groups = OnCreate.class)
        @Size(min = 3, max = 60, groups = OnCreate.class)
        String username,

        @Schema(description = "E-mail valido e unico", example = "cli1@eneng.local")
        @NotBlank(groups = OnCreate.class)
        @Email(groups = OnCreate.class)
        String email,

        @Schema(description = "Senha do usuario (sera hashed com BCrypt)", example = "segredo123",
                minLength = 6, maxLength = 120)
        @NotBlank(groups = OnCreate.class)
        @Size(min = 6, max = 120, groups = OnCreate.class)
        String password
) {
}
