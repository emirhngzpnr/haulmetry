CREATE TABLE processed_kafka_events (
                                        id BIGSERIAL PRIMARY KEY,
                                        event_id UUID NOT NULL,
                                        consumer_group VARCHAR(150) NOT NULL,
                                        processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                        CONSTRAINT uk_processed_kafka_event
                                            UNIQUE (event_id, consumer_group)
);