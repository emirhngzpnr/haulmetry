package com.truckpulse.telemetry.kafka;

import com.truckpulse.telemetry.event.DrivingEventCreatedEvent;
import com.truckpulse.telemetry.event.KafkaTopics;
import com.truckpulse.telemetry.service.DrivingEventProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DrivingEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(DrivingEventConsumer.class);

    private static final String CONSUMER_GROUP =
            "haulmetry-driving-event-log";

    private final DrivingEventProcessingService processingService;

    public DrivingEventConsumer(
            DrivingEventProcessingService processingService
    ) {
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = KafkaTopics.DRIVING_EVENT_CREATED,
            groupId = CONSUMER_GROUP
    )
    public void consume(
            ConsumerRecord<String, DrivingEventCreatedEvent> record
    ) {

        DrivingEventCreatedEvent event =
                record.value();

        boolean processed =
                processingService.process(
                        event,
                        CONSUMER_GROUP
                );

        if (!processed) {

            log.info(
                    "Duplicate driving event skipped. eventId={}, truckId={}, type={}, partition={}, offset={}",
                    event.eventId(),
                    event.truckId(),
                    event.type(),
                    record.partition(),
                    record.offset()
            );

            return;
        }

        log.info(
                "Driving event processed. eventId={}, truckId={}, type={}, key={}, partition={}, offset={}",
                event.eventId(),
                event.truckId(),
                event.type(),
                record.key(),
                record.partition(),
                record.offset()
        );
    }
}