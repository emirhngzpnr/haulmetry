package com.truckpulse.config;

import com.truckpulse.telemetry.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic telemetryReceivedTopic() {
        return TopicBuilder
                .name(KafkaTopics.TELEMETRY_RECEIVED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic telemetryReceivedDltTopic() {
        return TopicBuilder
                .name(KafkaTopics.TELEMETRY_RECEIVED_DLT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic drivingEventCreatedTopic() {
        return TopicBuilder
                .name(KafkaTopics.DRIVING_EVENT_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
