package com.eneng.suporte.api.dto;

import com.eneng.suporte.api.dto.validation.ValidationGroups.OnCreate;
import com.eneng.suporte.domain.model.QuestionCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de criacao de ticket do tipo QUESTION")
public record CreateQuestionRequest(
        @Schema(description = "Titulo da pergunta", example = "Como altero minha forma de pagamento?")
        @NotBlank(groups = OnCreate.class)
        @Size(min = 5, max = 120, groups = OnCreate.class)
        String title,

        @Schema(description = "Contexto detalhado da pergunta",
                example = "Quero trocar o cartao cadastrado sem cancelar a assinatura.")
        @NotBlank(groups = OnCreate.class)
        @Size(min = 10, max = 2000, groups = OnCreate.class)
        String description,

        @Schema(description = "Categoria da pergunta", example = "BILLING",
                allowableValues = {"BILLING", "USAGE", "ACCOUNT", "OTHER"})
        @NotNull(groups = OnCreate.class)
        QuestionCategory category
) {
}
