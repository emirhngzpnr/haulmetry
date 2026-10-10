package com.truckpulse.kafka.idempotency;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_kafka_events")
public class ProcessedKafkaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer_group", nullable = false)
    private String consumerGroup;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedKafkaEvent() {
    }

    public ProcessedKafkaEvent(
            UUID eventId,
            String consumerGroup,
            Instant processedAt
    ) {
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}