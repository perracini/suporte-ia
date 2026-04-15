package com.eneng.suporte.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.Duration;

@Entity
@DiscriminatorValue("FEATURE")
public class FeatureTicket extends Ticket {

    @Column(name = "business_value", length = 2000)
    private String businessValue;

    @Column(name = "target_version", length = 40)
    private String targetVersion;

    public FeatureTicket() {
    }

    @Override
    public Duration slaAlvo() {
        return Duration.ofDays(15);
    }

    @Override
    public Priority prioridadeInicialSugerida() {
        return Priority.MEDIUM;
    }

    @Override
    public String promptParaTriagem() {
        return """
                Tipo: FEATURE REQUEST
                Valor de negocio: %s
                Versao alvo: %s
                Titulo: %s
                Descricao: %s

                Classifique esta feature em uma categoria curta, sugira uma prioridade entre
                LOW, MEDIUM, HIGH, CRITICAL e estime esforco qualitativo como S, M, L ou XL no
                rascunho de resposta. Responda em JSON com as chaves suggestedCategory,
                suggestedPriority, draftReply, confidence (0..1).
                """.formatted(
                        businessValue == null ? "n/a" : businessValue,
                        targetVersion == null ? "n/a" : targetVersion,
                        getTitle(),
                        getDescription()
                );
    }

    @Override
    public String ticketType() {
        return "FEATURE";
    }

    public String getBusinessValue() {
        return businessValue;
    }

    public void setBusinessValue(String businessValue) {
        this.businessValue = businessValue;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }
}
