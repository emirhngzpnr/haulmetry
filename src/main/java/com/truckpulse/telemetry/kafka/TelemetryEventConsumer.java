package com.truckpulse.telemetry.kafka;

import com.truckpulse.telemetry.event.KafkaTopics;
import com.truckpulse.telemetry.event.TelemetryReceivedEvent;
import com.truckpulse.telemetry.service.TelemetryEventProcessingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryEventConsumer {
    private static final Logger log =
            LoggerFactory.getLogger(TelemetryEventConsumer.class);

    private static final String CONSUMER_GROUP =
            "haulmetry-telemetry-log";

    private final TelemetryEventProcessingService processingService;

    public TelemetryEventConsumer(TelemetryEventProcessingService processingService) {
        this.processingService = processingService;

    }

    @KafkaListener(
            topics = KafkaTopics.TELEMETRY_RECEIVED,
            groupId = CONSUMER_GROUP
    )
    public void consume(
            ConsumerRecord<String, TelemetryReceivedEvent> record
    ) {

        TelemetryReceivedEvent event =
                record.value();

        boolean processed =
                processingService.process(
                        event,
                        CONSUMER_GROUP
                );
        if (!processed) {
            log.info(
                    "Duplicate telemetry event skipped. eventId={}, truckId={}, partition={}, offset={}",
                    event.eventId(),
                    event.truckId(),
                    record.partition(),
                    record.offset()
            );

            return;
        }
        log.info(
                "Telemetry event consumed. eventId={}, truckId={}, key={}, partition={}, offset={}, sequence={}",
                event.eventId(),
                event.truckId(),
                record.key(),
                record.partition(),
                record.offset(),
                event.sequenceNumber()
        );
    }
}
