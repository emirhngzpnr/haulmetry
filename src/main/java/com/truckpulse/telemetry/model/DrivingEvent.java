package com.truckpulse.telemetry.model;

public record DrivingEvent(
        String truckId,
        DrivingEventType eventType,
        double previousSpeed,
        double currentSpeed,
        double speedDifference,
        long durationMs,
        double deceleration

) {
}
