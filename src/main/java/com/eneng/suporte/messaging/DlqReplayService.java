package com.eneng.suporte.messaging;

import com.eneng.suporte.api.dto.DlqEntryResponse;
import com.eneng.suporte.config.KafkaAppProperties;
import com.eneng.suporte.domain.model.Ticket;
import com.eneng.suporte.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DlqReplayService {

    private static final Logger log = LoggerFactory.getLogger(DlqReplayService.class);

    private final TicketTriageDlqListener dlqListener;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaAppProperties properties;
    private final TicketRepository ticketRepository;

    public DlqReplayService(TicketTriageDlqListener dlqListener,
                            KafkaTemplate<String, Object> kafkaTemplate,
                            KafkaAppProperties properties,
                            TicketRepository ticketRepository) {
        this.dlqListener = dlqListener;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.ticketRepository = ticketRepository;
    }

    public int replay() {
        int count = 0;
        TicketCreatedEvent event;
        while ((event = dlqListener.pendingReplay().poll()) != null) {
            log.info("Reprocessando evento da DLQ ticketId={}", event.ticketId());
            kafkaTemplate.send(properties.topics().ticketCreated(), event.ticketId().toString(), event);
            count++;
        }
        return count;
    }

    @Transactional(readOnly = true)
    public List<DlqEntryResponse> list() {
        List<DlqEntryResponse> result = new ArrayList<>();
        for (TicketCreatedEvent event : dlqListener.pendingReplay()) {
            String title = ticketRepository.findById(event.ticketId())
                    .map(Ticket::getTitle)
                    .orElse(null);
            result.add(new DlqEntryResponse(
                    event.ticketId(),
                    event.ticketType(),
                    title,
                    event.createdBy(),
                    event.createdAt()
            ));
        }
        return result;
    }
}
