package com.truckpulse.kafka.idempotency;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class KafkaEventIdempotencyService {

    private final ProcessedKafkaEventRepository repository;

    public KafkaEventIdempotencyService(
            ProcessedKafkaEventRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public boolean tryMarkAsProcessed(
            UUID eventId,
            String consumerGroup
    ) {
        int inserted = repository.insertIfAbsent(
                eventId,
                consumerGroup,
                Instant.now()
        );

        return inserted == 1;
    }
}