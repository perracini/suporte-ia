package com.eneng.suporte.unit.domain;

import com.eneng.suporte.domain.model.Priority;
import com.eneng.suporte.domain.model.QuestionCategory;
import com.eneng.suporte.domain.model.QuestionTicket;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTicketTest {

    @Test
    void sla_2_dias_prioridade_low() {
        QuestionTicket q = new QuestionTicket();
        assertThat(q.slaAlvo()).isEqualTo(Duration.ofDays(2));
        assertThat(q.prioridadeInicialSugerida()).isEqualTo(Priority.LOW);
    }

    @Test
    void prompt_inclui_categoria() {
        QuestionTicket q = new QuestionTicket();
        q.setTitle("Como troco cartao?");
        q.setDescription("Preciso alterar forma de pagamento");
        q.setCategory(QuestionCategory.BILLING);

        assertThat(q.promptParaTriagem())
                .contains("PERGUNTA")
                .contains("BILLING")
                .contains("Como troco cartao?");
    }

    @Test
    void ticketType_retorna_QUESTION() {
        assertThat(new QuestionTicket().ticketType()).isEqualTo("QUESTION");
    }
}
