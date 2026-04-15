package com.eneng.suporte.unit.domain;

import com.eneng.suporte.domain.model.BugTicket;
import com.eneng.suporte.domain.model.Priority;
import com.eneng.suporte.domain.model.Severity;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BugTicketTest {

    @Test
    void blocker_deve_ter_sla_4h_e_prioridade_critical() {
        BugTicket ticket = new BugTicket();
        ticket.setSeverity(Severity.BLOCKER);

        assertThat(ticket.slaAlvo()).isEqualTo(Duration.ofHours(4));
        assertThat(ticket.prioridadeInicialSugerida()).isEqualTo(Priority.CRITICAL);
    }

    @Test
    void trivial_deve_ter_sla_7d_e_prioridade_low() {
        BugTicket ticket = new BugTicket();
        ticket.setSeverity(Severity.TRIVIAL);

        assertThat(ticket.slaAlvo()).isEqualTo(Duration.ofDays(7));
        assertThat(ticket.prioridadeInicialSugerida()).isEqualTo(Priority.LOW);
    }

    @Test
    void severidade_ausente_usa_defaults() {
        BugTicket ticket = new BugTicket();

        assertThat(ticket.slaAlvo()).isEqualTo(Duration.ofDays(3));
        assertThat(ticket.prioridadeInicialSugerida()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void prompt_inclui_todos_campos_relevantes() {
        BugTicket ticket = new BugTicket();
        ticket.setTitle("Login quebrado");
        ticket.setDescription("Nao consigo logar");
        ticket.setStepsToReproduce("Abrir pagina; Clicar login");
        ticket.setAffectedVersion("1.2.3");
        ticket.setSeverity(Severity.MAJOR);

        String prompt = ticket.promptParaTriagem();

        assertThat(prompt)
                .contains("BUG")
                .contains("MAJOR")
                .contains("1.2.3")
                .contains("Login quebrado")
                .contains("Nao consigo logar")
                .contains("Abrir pagina");
    }

    @Test
    void ticketType_retorna_BUG() {
        assertThat(new BugTicket().ticketType()).isEqualTo("BUG");
    }
}
