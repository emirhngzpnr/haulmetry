package com.truckpulse.telemetry.service;

import com.truckpulse.kafka.idempotency.KafkaEventIdempotencyService;
import com.truckpulse.telemetry.event.TelemetryReceivedEvent;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class TelemetryEventProcessingService {
    private final KafkaEventIdempotencyService idempotencyService;

    public TelemetryEventProcessingService(
            KafkaEventIdempotencyService idempotencyService
    ) {
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public boolean process(
            TelemetryReceivedEvent event,
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
         * İleride bu event'e ait gerçek business işlemleri
         * burada yapılacak.
         *
         * Örneğin:
         * - sürüş skoru hesaplama
         * - analiz kaydı oluşturma
         * - istatistik güncelleme
         */

        return true;
    }
}
