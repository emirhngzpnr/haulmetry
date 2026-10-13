
package com.truckpulse.kafka.outbox;

import com.truckpulse.telemetry.event.DrivingEventCreatedEvent;
import com.truckpulse.telemetry.event.TelemetryReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            JsonMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /*
     * Önceki çalışma bittikten 2 saniye sonra
     * bekleyen outbox kayıtlarını kontrol eder.
     */
    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {

        log.info("Outbox Publisher taraması başladı.");

        List<OutboxEvent> pendingEvents =
                outboxEventRepository
                        .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        log.info(
                "Outbox Publisher bekleyen olay sayısı={}",
                pendingEvents.size()
        );

        for (OutboxEvent event : pendingEvents) {

            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEvent event) {

        Object payload;

        /*
         * Outbox'ta JSON metni saklıyoruz.
         * Kafka'ya göndermeden önce onu doğru
         * event nesnesine dönüştürüyoruz.
         */
        try {
            payload = deserializeEvent(event);

        } catch (Exception exception) {

            registerFailure(event, exception);
            return;
        }

        /*
         * Kafka gönderiminin sonucunu bekliyoruz.
         * Başarılı gönderim onaylanmadan kaydı
         * yayımlanmış olarak işaretlemiyoruz.
         */
        try {
            log.info(
                    "Outbox olayı Kafka'ya gönderiliyor. id={}, topic={}",
                    event.getId(),
                    event.getTopic()
            );

            var result = kafkaTemplate.send(
                    event.getTopic(),
                    event.getAggregateId(),
                    payload
            ).get();

            event.markPublished(Instant.now());
            outboxEventRepository.save(event);

            log.info(
                    "Outbox event published. id={}, type={}, topic={}, partition={}, offset={}",
                    event.getId(),
                    event.getEventType(),
                    event.getTopic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();
            registerFailure(event, exception);

        } catch (Exception exception) {

            registerFailure(event, exception);
        }
    }

    private Object deserializeEvent(OutboxEvent event)
            throws JacksonException {

        return switch (event.getEventType()) {

            case "TelemetryReceivedEvent" ->
                    objectMapper.readValue(
                            event.getPayload(),
                            TelemetryReceivedEvent.class
                    );

            case "DrivingEventCreatedEvent" ->
                    objectMapper.readValue(
                            event.getPayload(),
                            DrivingEventCreatedEvent.class
                    );

            default -> throw new IllegalArgumentException(
                    "Unsupported outbox event type: "
                            + event.getEventType()
            );
        };
    }

    private void registerFailure(
            OutboxEvent event,
            Exception exception
    ) {

        String errorMessage = exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getSimpleName();

        event.registerFailure(errorMessage);
        outboxEventRepository.save(event);

        log.error(
                "Outbox event could not be published. id={}, type={}, topic={}, attempts={}",
                event.getId(),
                event.getEventType(),
                event.getTopic(),
                event.getAttempts(),
                exception
        );
    }
}
