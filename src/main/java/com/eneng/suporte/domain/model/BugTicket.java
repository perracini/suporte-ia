package com.eneng.suporte.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Duration;

@Entity
@DiscriminatorValue("BUG")
public class BugTicket extends Ticket {

    @Column(name = "steps_to_reproduce", length = 2000)
    private String stepsToReproduce;

    @Column(name = "affected_version", length = 40)
    private String affectedVersion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    public BugTicket() {
    }

    @Override
    public Duration slaAlvo() {
        if (severity == null) {
            return Duration.ofDays(3);
        }
        return switch (severity) {
            case BLOCKER -> Duration.ofHours(4);
            case MAJOR -> Duration.ofDays(1);
            case MINOR -> Duration.ofDays(3);
            case TRIVIAL -> Duration.ofDays(7);
        };
    }

    @Override
    public Priority prioridadeInicialSugerida() {
        if (severity == null) {
            return Priority.MEDIUM;
        }
        return switch (severity) {
            case BLOCKER -> Priority.CRITICAL;
            case MAJOR -> Priority.HIGH;
            case MINOR -> Priority.MEDIUM;
            case TRIVIAL -> Priority.LOW;
        };
    }

    @Override
    public String promptParaTriagem() {
        return """
                Tipo: BUG
                Severidade: %s
                Versao afetada: %s
                Titulo: %s
                Descricao: %s
                Passos para reproduzir: %s

                Classifique este bug em uma categoria curta (ex.: auth, ui, performance, data, integration),
                sugira uma prioridade entre LOW, MEDIUM, HIGH, CRITICAL e escreva um rascunho curto de
                resposta para o cliente informando que o time vai investigar. Responda em JSON com as chaves
                suggestedCategory, suggestedPriority, draftReply, confidence (0..1).
                """.formatted(
                        severity == null ? "UNKNOWN" : severity.name(),
                        affectedVersion == null ? "n/a" : affectedVersion,
                        getTitle(),
                        getDescription(),
                        stepsToReproduce == null ? "n/a" : stepsToReproduce
                );
    }

    @Override
    public String ticketType() {
        return "BUG";
    }

    public String getStepsToReproduce() {
        return stepsToReproduce;
    }

    public void setStepsToReproduce(String stepsToReproduce) {
        this.stepsToReproduce = stepsToReproduce;
    }

    public String getAffectedVersion() {
        return affectedVersion;
    }

    public void setAffectedVersion(String affectedVersion) {
        this.affectedVersion = affectedVersion;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }
}
