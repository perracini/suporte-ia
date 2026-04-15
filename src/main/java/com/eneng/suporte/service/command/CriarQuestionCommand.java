package com.eneng.suporte.service.command;

import com.eneng.suporte.domain.model.QuestionCategory;

public record CriarQuestionCommand(
        String title,
        String description,
        QuestionCategory category
) {
}
