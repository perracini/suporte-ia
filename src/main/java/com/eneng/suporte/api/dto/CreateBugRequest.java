package com.eneng.suporte.api.dto;

import com.eneng.suporte.api.dto.validation.ValidationGroups.OnCreate;
import com.eneng.suporte.domain.model.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de criacao de ticket do tipo BUG")
public record CreateBugRequest(
        @Schema(description = "Titulo curto do bug", example = "Falha no login com Google")
        @NotBlank(groups = OnCreate.class)
        @Size(min = 5, max = 120, groups = OnCreate.class)
        String title,

        @Schema(description = "Descricao detalhada do problema",
                example = "Ao clicar em Entrar com Google, a tela fica em branco e nao redireciona.")
        @NotBlank(groups = OnCreate.class)
        @Size(min = 10, max = 2000, groups = OnCreate.class)
        String description,

        @Schema(description = "Passos para reproduzir o bug",
                example = "1. Abrir /login; 2. Clicar em Google; 3. Aguardar")
        @Size(max = 2000, groups = OnCreate.class)
        String stepsToReproduce,

        @Schema(description = "Versao afetada do produto", example = "1.4.2")
        @Size(max = 40, groups = OnCreate.class)
        String affectedVersion,

        @Schema(description = "Severidade reportada pelo cliente", example = "MAJOR",
                allowableValues = {"TRIVIAL", "MINOR", "MAJOR", "BLOCKER"})
        @NotNull(groups = OnCreate.class)
        Severity severity
) {
}
