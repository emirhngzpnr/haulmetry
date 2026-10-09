package com.truckpulse.telemetry.event;

import java.time.Instant;
import java.util.UUID;

public record TelemetryReceivedEvent(
        UUID eventId,
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
