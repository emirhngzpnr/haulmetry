package com.truckpulse.telemetry.kafka;

import com.truckpulse.telemetry.event.DrivingEventCreatedEvent;
import com.truckpulse.telemetry.event.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DrivingEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(DrivingEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DrivingEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDrivingEventCreated(
            DrivingEventCreatedEvent event
    ) {

        kafkaTemplate.send(
                KafkaTopics.DRIVING_EVENT_CREATED,
                event.truckId(),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {
                log.error(
                        "Failed to publish driving event. eventId={}, truckId={}, type={}",
                        event.eventId(),
                        event.truckId(),
                        event.type(),
                        exception
                );

                return;
            }

            log.info(
                    "Driving event published. eventId={}, truckId={}, type={}, partition={}, offset={}",
                    event.eventId(),
                    event.truckId(),
                    event.type(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        });
    }
}