package com.eneng.suporte.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Duration;

@Entity
@DiscriminatorValue("QUESTION")
public class QuestionTicket extends Ticket {

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuestionCategory category;

    private Boolean answered = false;

    public QuestionTicket() {
    }

    @Override
    public Duration slaAlvo() {
        return Duration.ofDays(2);
    }

    @Override
    public Priority prioridadeInicialSugerida() {
        return Priority.LOW;
    }

    @Override
    public String promptParaTriagem() {
        return """
                Tipo: PERGUNTA
                Categoria informada: %s
                Titulo: %s
                Descricao: %s

                Classifique esta pergunta em uma categoria curta e escreva um rascunho de resposta
                direto em ate 3 paragrafos. Sugira prioridade entre LOW, MEDIUM, HIGH, CRITICAL.
                Responda em JSON com as chaves suggestedCategory, suggestedPriority, draftReply,
                confidence (0..1).
                """.formatted(
                        category == null ? "OTHER" : category.name(),
                        getTitle(),
                        getDescription()
                );
    }

    @Override
    public String ticketType() {
        return "QUESTION";
    }

    public QuestionCategory getCategory() {
        return category;
    }

    public void setCategory(QuestionCategory category) {
        this.category = category;
    }

    public Boolean getAnswered() {
        return answered;
    }

    public void setAnswered(Boolean answered) {
        this.answered = answered;
    }
}
