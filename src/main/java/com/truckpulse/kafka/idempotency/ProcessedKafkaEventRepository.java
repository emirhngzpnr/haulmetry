package com.truckpulse.kafka.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedKafkaEventRepository
        extends JpaRepository<ProcessedKafkaEvent, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT INTO processed_kafka_events
                        (event_id, consumer_group, processed_at)
                    VALUES
                        (:eventId, :consumerGroup, :processedAt)
                    ON CONFLICT (event_id, consumer_group)
                    DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("consumerGroup") String consumerGroup,
            @Param("processedAt") Instant processedAt
    );
}