package com.truckpulse.telemetry.service;

import com.truckpulse.kafka.idempotency.KafkaEventIdempotencyService;
import com.truckpulse.telemetry.event.DrivingEventCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DrivingEventProcessingService {

    private final KafkaEventIdempotencyService idempotencyService;

    public DrivingEventProcessingService(
            KafkaEventIdempotencyService idempotencyService
    ) {
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public boolean process(
            DrivingEventCreatedEvent event,
            String consumerGroup
    ) {

        boolean firstProcessing =
                idempotencyService.tryMarkAsProcessed(
                        event.eventId(),
                        consumerGroup
                );

        if (!firstProcessing) {
            return false;
        }

        /*
         * Daha sonra burada gerçek downstream business logic olacak:
         *
         * - driver score update
         * - fleet analytics
         * - alert generation
         * - statistics update
         */

        return true;
    }
}