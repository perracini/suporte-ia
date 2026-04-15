package com.eneng.suporte.api.dto;

import com.eneng.suporte.api.dto.validation.ValidationGroups.OnCreate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de criacao de ticket do tipo FEATURE")
public record CreateFeatureRequest(
        @Schema(description = "Titulo da feature solicitada", example = "Exportacao de relatorios em CSV")
        @NotBlank(groups = OnCreate.class)
        @Size(min = 5, max = 120, groups = OnCreate.class)
        String title,

        @Schema(description = "Descricao detalhada da feature",
                example = "Precisamos exportar o relatorio mensal em CSV alem do PDF atual.")
        @NotBlank(groups = OnCreate.class)
        @Size(min = 10, max = 2000, groups = OnCreate.class)
        String description,

        @Schema(description = "Valor de negocio esperado",
                example = "Reduz tempo de fechamento contabil em 40% para clientes enterprise.")
        @Size(max = 2000, groups = OnCreate.class)
        String businessValue,

        @Schema(description = "Versao alvo para entrega", example = "2.0")
        @Size(max = 40, groups = OnCreate.class)
        String targetVersion
) {
}
