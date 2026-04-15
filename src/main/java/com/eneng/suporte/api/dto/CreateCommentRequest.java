package com.eneng.suporte.api.dto;

import com.eneng.suporte.api.dto.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de criacao de comentario em um ticket")
public record CreateCommentRequest(
        @Schema(description = "Conteudo do comentario", example = "Estamos investigando, aguardem retorno.")
        @NotBlank(groups = OnCreate.class)
        @Size(min = 1, max = 2000, groups = OnCreate.class)
        String body,

        @Schema(description = "Se true, o comentario e interno (visivel apenas para AGENT/ADMIN)",
                example = "false", defaultValue = "false")
        boolean internal
) {
}
