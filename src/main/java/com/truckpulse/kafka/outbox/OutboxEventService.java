
package com.truckpulse.kafka.outbox;

import tools.jackson.core.JacksonException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper objectMapper;

    public OutboxEventService(
            OutboxEventRepository outboxEventRepository,
            JsonMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveEvent(
            String aggregateType,
            String aggregateId,
            String topic,
            Object event
    ) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent(
                    UUID.randomUUID(),
                    aggregateType,
                    aggregateId,
                    event.getClass().getSimpleName(),
                    topic,
                    payload,
                    Instant.now()
            );

            outboxEventRepository.save(outboxEvent);

        }
        catch (JacksonException exception) {
        throw new IllegalStateException(
                "Outbox event JSON'a dönüştürülemedi.",
                exception
        );
    }
    }
}
