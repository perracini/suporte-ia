package com.eneng.suporte.unit.domain;

import com.eneng.suporte.domain.model.FeatureTicket;
import com.eneng.suporte.domain.model.Priority;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureTicketTest {

    @Test
    void sla_fixo_15_dias() {
        assertThat(new FeatureTicket().slaAlvo()).isEqualTo(Duration.ofDays(15));
    }

    @Test
    void prioridade_inicial_medium() {
        assertThat(new FeatureTicket().prioridadeInicialSugerida()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void prompt_menciona_feature_request() {
        FeatureTicket ticket = new FeatureTicket();
        ticket.setTitle("Exportar CSV");
        ticket.setDescription("Cliente quer exportar relatorio");
        ticket.setBusinessValue("Aumento de retencao");
        ticket.setTargetVersion("2.0");

        String prompt = ticket.promptParaTriagem();

        assertThat(prompt)
                .contains("FEATURE REQUEST")
                .contains("Exportar CSV")
                .contains("Aumento de retencao")
                .contains("2.0");
    }

    @Test
    void ticketType_retorna_FEATURE() {
        assertThat(new FeatureTicket().ticketType()).isEqualTo("FEATURE");
    }
}
