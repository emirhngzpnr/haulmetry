package com.truckpulse.telemetry.event;

public final class KafkaTopics {
    private KafkaTopics() {
    }

    public static final String TELEMETRY_RECEIVED =
            "haulmetry.telemetry.received";

    public static final String TELEMETRY_RECEIVED_DLT =
            "haulmetry.telemetry.received-dlt";
}
