package com.truckpulse.telemetry.event;

import java.time.Instant;

public record TelemetryReceivedEvent(
        String truckId,
        String sessionId,
        double speed,
        int rpm,
        double fuel,
        int gear,
        long sequenceNumber,
        Instant timestamp
) {
}
