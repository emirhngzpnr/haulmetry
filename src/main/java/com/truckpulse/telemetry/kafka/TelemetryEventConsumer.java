package com.truckpulse.telemetry.kafka;

import com.truckpulse.telemetry.event.KafkaTopics;
import com.truckpulse.telemetry.event.TelemetryReceivedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TelemetryEventConsumer {
    private static final Logger log =
            LoggerFactory.getLogger(TelemetryEventConsumer.class);

    @KafkaListener(
            topics = KafkaTopics.TELEMETRY_RECEIVED,
            groupId = "haulmetry-telemetry-log"
    )
    public void consume(
            ConsumerRecord<String, TelemetryReceivedEvent> record
    ) {

        TelemetryReceivedEvent event =
                record.value();


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
