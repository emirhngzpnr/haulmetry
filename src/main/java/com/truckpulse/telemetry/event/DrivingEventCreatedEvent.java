package com.truckpulse.telemetry.event;

import com.truckpulse.telemetry.model.DrivingEventType;

import java.time.Instant;
import java.util.UUID;

public record DrivingEventCreatedEvent(
        UUID eventId,
        String truckId,
        DrivingEventType type,
        double previousSpeed,
        double currentSpeed,
        double speedDifference,
        Instant timestamp
) {
}
