package com.truckpulse.telemetry.dto;

import com.truckpulse.telemetry.model.DrivingEventType;

import java.time.Instant;

public record DrivingEventResponse(Long id,
                                   String truckId,
                                   Long tripId,
                                   DrivingEventType eventType,
                                   double previousSpeed,
                                   double currentSpeed,
                                   double speedDifference,
                                   long durationMs,
                                   double deceleration,
                                   Instant occurredAt) {
}
