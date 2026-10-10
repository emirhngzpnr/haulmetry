package com.truckpulse.telemetry.service;

import com.truckpulse.telemetry.event.TelemetryReceivedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TelemetryEventProcessingServiceTest {

    @Autowired
    private TelemetryEventProcessingService processingService;

    @Test
    void shouldProcessEventOnlyOncePerConsumerGroup() {

        UUID eventId = UUID.randomUUID();

        TelemetryReceivedEvent event =
                new TelemetryReceivedEvent(
                        eventId,
                        "TRUCK-001",
                        "test-session",
                        80.0,
                        1500,
                        400.0,
                        10,
                        1L,
                        Instant.now()
                );

        boolean firstProcessing =
                processingService.process(
                        event,
                        "haulmetry-telemetry-log"
                );

        boolean duplicateProcessing =
                processingService.process(
                        event,
                        "haulmetry-telemetry-log"
                );

        boolean differentConsumerGroup =
                processingService.process(
                        event,
                        "haulmetry-driver-scoring"
                );

        assertTrue(firstProcessing);
        assertFalse(duplicateProcessing);
        assertTrue(differentConsumerGroup);
    }
}