package com.truckpulse.telemetry.model;

public record TelemetrySessionKey(
        String truckId,
        String sessionId
) {
}
