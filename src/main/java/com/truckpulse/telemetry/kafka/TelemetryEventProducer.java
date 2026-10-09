package com.truckpulse.telemetry.kafka;

import com.truckpulse.telemetry.event.KafkaTopics;
import com.truckpulse.telemetry.event.TelemetryReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TelemetryEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(TelemetryEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TelemetryEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTelemetryReceived(
            TelemetryReceivedEvent event
    ) {

        kafkaTemplate.send(
                KafkaTopics.TELEMETRY_RECEIVED,
                event.truckId(),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {

                log.error(
                        "Failed to publish telemetry event for truck {}.",
                        event.truckId(),
                        exception
                );

                return;
            }

            log.info(
                    "Telemetry event published. truckId={}, partition={}, offset={}",
                    event.truckId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }
}